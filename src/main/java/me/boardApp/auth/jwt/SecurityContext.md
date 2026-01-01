# Spring Security

## SecurityContext

"인증은 됐는데 SecurityContext가 존재할 수도 있고, 항상 신뢰할 수는 없다"  
이 말의 정확한 의미 👇

❌ ”인증이 되었다 = 항상 완전한 SecurityContext가 있다"  
⭕️ “인가 판단이 일어났을 때, SecurityContext가 부분적이거나 불완전한 상태일 수 있다”

---

### "인증됐다"는 말이 두가지 의미로 쓰인다

#### 의미 1: 보안 정책 관점의 인증

- 이 요청은 익명 요청이 아님
- authenticated() 규칙에 걸렸다
- 필터 체인을 통과했다

#### 의미 2: SecurityContext 관점의 인증

- SecurityContextHolder에
- 정상적인 Authentication 객체
- 정상적인 principal(CustomUserDetails) 가 들어 있다

👉 이 두개는 항상 같지 않다

---

### "인증은 됐는데 userId가 없는" 실제 케이스들

실제 운영에서 자주 발생하는 상황들

#### ✅케이스 1 : 토큰은 유효하지만, 유저는 사라짐

1. JWT는 서명/만료 모두 OK
2. userId 추출 성공
3. DB에서 user 조회 -> ❌ (탈퇴 / 삭제됨)

이 경우 :

- 인증 필터는 토큰 자체는 통과
- 하지만 principal을 세팅하지 않거나
- 예외가 중간에서 발생할 수 있음  
  👉AccessDeniedHandler까지 올 수도 있음  
  👉**SecurityContext가 불완전**

---

#### ✅케이스 2 : 필터 도중 예외 발생

ClientContextFilter  
→ JwtAuthenticationFilter (여기서 예외)  
→ ExceptionTranslationFilter  
→ AccessDeniedHandler

이 흐름에서 :

- SecurityContext가 아직 안 채워졌을 수도 있음
- authentication == null 가능

---

#### ✅케이스 3 : 익명 Authentication이 들어 있는 경우

Spring Security 는 이걸 자동으로 함 👇

```java
authentication =new

AnonymousAuthenticationToken(...)
```

이건 :

- authentication != null
- isAuthenticated() == true
- ❌실제 유저가 아님

그래서 이 체크가 필요 👇

```java
!(authentication instanceof AnonymousAuthenticationToken)
```

---

### 인증 실패 vs 인가 실패는 감사 의미가 다르다

| 상황          | 감사의미                      |
|-------------|---------------------------|
| 인증 실패 (401) | "누가 들어오려다 실패했다"           |
| 인가 실패 (403) | "**이미 들어온 누군가가** 권한을 넘었다" |

#### ACCESS_DENIED는 이런 의미

- 로그인은 했음
- 토큰도 유효함
- 근데 접근하면 안되는 리소스를 건드림  
  👉 이건 **보안적으로 더 중요한 사건**

그래서 :

- userId가 있으면 -> "정상 계정의 권한 오용"
- userId가 없으면 -> "비정상 상태에서의 접근 시도"

둘 다 감사 대상

---

## 한 문장 정리

인가 실패 시점의 SecurityContext는  
'있을 수도 있고, 없을 수도 있고, 불완전할 수도 있다'  
그래서 감사 로그는 항상 방어적으로 작성해야 한다

---

### 공부

JwtAuthenticationFilter를 거쳤다는 것과  
SecurityContext가 완성됐다는 것은 전혀 다른 말

우리는 JwtAuthenticationFilter에서 setAuthentication (Security Context 완성 전)전에 실패하면 EntryPoint로 가게 만들었지만,  
그게 "항상 즉시 체인이 끊긴다" 는 뜻은 아니다

심지어 익명 요청에도 허용을 해논 상태라 다음 필터로 건너갈 수 있다

JwtAuthenticationFilter는 인증을 '보장'하지 않는다  
오직 '시도'만 보장한다

---

## JwtAuthenticationEntryPoint 에서 감사 남기기 vs AuthService에서 감사 남기기

AuthService에서 이미 남긴 감사 로그와 EntryPoint 감사 로그는 역할이 다르기 때문에 중복이 아니다

### AuthService에서 남긴 감사는 "비즈니스 판단 로그"

```java
securityEventService.record(
	SecurityEventType.LOGIN_FAIL,
	userId,
	clientContext
	);
```

#### ✅AuthService가 호출됐다는 전제

- 컨트롤러까지 도달함
- 요청 구조가 정상
- 인증 API (/auth/login, /auth/refresh) 자체는 통과

즉, "인증 로직 안으로 들어온 이후의 판단 결과"

### EntryPoint는 AuthService보다 훨씬 앞단이다

Spring Security 흐름 👇

```text
Client
 ↓
Filter (JwtAuthenticationFilter)
 ↓
ExceptionTranslationFilter
 ↓
AuthenticationEntryPoint (401)
```

#### EntryPoint가 호출된다는 뜻은?

👉 AuthService까지 아예 도달하지 못했다

즉 :

- 컨트롤러 ❌
- 서비스 ❌
- 비즈니스 로직 ❌

#### EntryPoint에서 발생하는 실패는 "프로토콜 / 보안 계층 실패"

EntryPoint에서 다뤄야하는 케이스는 이것:

##### 🔐 토큰 자체가 문제인 경우

- 서명 위조
- 토큰 형식 깨짐
- 토큰 만료
- 토큰 없음 (보호된 자원 접근 시)

예:

```http request
Authorization: Bearer asdfasdf
```

이건 :

- 로그인 실패 ❌
- 비밀번호 실패 ❌
- 사용자 검증 실패 ❌  
  👉 "애초에 인증 객체를 만들 수 없는 요청"  
  그래서 이건 AuthService에서 절대 잡을 수 없다

---

### 그래서 감사 로그가 나뉨

#### 🟢AuthService 감사

"인증 로직 내부에서 일어난 일"

- LOGIN_FAIL
- LOCKED_ACCOUNT_LOGIN_ATTEMPT
- REFRESH_REUSED
- ACCOUNT_LOCKED

#### 🔴EntryPoint 감사

"인증을 시도했지만 인증 객체 자체를 만들 수 없었던 일"

- TOKEN_INVALID
- TOKEN_EXPIRED
- TOKEN_MALFORMED
- UNAUTHORIZED_REQUEST

### 정리

| 위치                  | 의미            | 감사 대상           |
|---------------------|---------------|-----------------|
| AuthService         | 인증 정책 판단      | 정상 API 요청 내부 실패 |
| EntryPoint          | 인증 불가         | 토큰 위조/만료/없음     |
| AccessDeniedHandler | 인증은 됐으나 권한 없음 | 권한 상승 시도        |


---

## 1230 정리
### 1️⃣오늘의 핵심 한줄
우리는 "로그를 찍은게 아니라, 보안 감사(Audit)를 설계했다"
- 단순 log.warn() ❌
- **보안 이벤트를 구조화해서 DB에 남기는 Audit 시스템** ⭕️

---
### 2️⃣로그 (log) vs 감사 (Audit) 정확한 구분
#### 🔹log (텍스트 로그)
- 목적 : 운영/ 디버깅
- 형태 : 문자열
- 위치 : 파일, 콘솔, ELK
- 한계 : 
  - 유저병 / IP별 조회 어려움
  - "누가, 언제, 왜" 분석 힘듬

#### 🔹audit (SecurityEvent)
- 목적 : **보안 사건 기록 + 추적 + 증거**
- 형태 : 구조화된 데이터 (DB)
- 장점 : 
  - 관리자 화면 가능
  - 통계 / 임계치 / 정책 근거 가능  

---
### 3️⃣전체 보안 이벤트 흐름 
```text
Client Request
   ↓
ClientContextFilter      (요청 환경 수집)
   ↓
JwtAuthenticationFilter  (인증 시도)
   ↓
인가 / 예외 발생
   ↓
EntryPoint / AccessDeniedHandler (정책 경계)
   ↓
SecurityEventService     (Audit 기록)
   ↓
SecurityEvent (DB)
```
중요 포인트
- Filter는 판단 ❌ -> **수집만**
- Service는 HTTP 모름
- **정책 경계(EntryPoint / DeniedHandler)** 에서 감사 남김

---
### 4️⃣ClientContext 설계 정리
#### 왜 만들었었나? 
- HttpServletRequest 너무 큼
- 서비스 계층이 HTTP에 의존하면 ❌

#### 해결
```java
public record ClientContext(
    String ipAddress,
    String userAgent,
    String requestUri,
    String httpMethod
)
```
#### 어디서 만드나? 
- ✅Filter
- ❌Controller / Service
```java
request.setAttribute(CLIENT_CONTEXT_KEY, ClientContext.from(request));
```
#### Controller에서 꺼낼 때 캐스팅하는 이유
```java
(ClientContext) request.getAttribute(...)
```
- getAttribute()는 **무조건 Object 반환**
- 재생성 ❌, **같은 객체 참조에 타입만 알려주는 것**

---
### 5️⃣"ClientContext가 null일 수 있다"의 진짜 의미
일반 웹 요청에서는 : 
- ❌거의 없음  

하지만 가능성은 존재 :
- 테스트 코드에서 Service 직접 호출
- 비HTTP 트리거 (배치, 스케줄러)
- Filter 제외된 요청  

그래서 :

```java
// SecurityEventService 에서 
if(context ==null){
context =ClientContext.

system();
}
```
👉 **방어적 설계 = 좋은 설계**

---
### 6️⃣ 인증/인가/SecurityContext 
#### 🔹익명 요청은 "막힌 요청"이 아님
- permitAll() 때문에 **필터 체인 통과 가능**
- SecurityContext에 **AnonymousAuthenticationToken** 들어감

#### 🔹흐름 요약
| 상황         | 결과                        |
|------------|---------------------------|
| 토큰 없음      | 익명 인증                     |
| 토큰 이상      | EntryPoint (401)          |
| 인증 O, 권한 X | AccessDeniedHandler (403) |
👉**인가 단계에서 인증 여부를 다시 확인하는게 정상 구조**

---
### 7️⃣ EntryPoint/DeniedHandler에서 Audit 남기는 이유
#### 왜 Controller가 아니라 여기인가? 
- Controller까지 오면 **이미 정책 위반이 아님**
- 인증/인가 실패는 **정책 경계에서 발생**  

👉 **정책이 깨진 지점"에서 감사 기록**

--- 
### 8️⃣오늘 가장 중요한 설계 ExceptionCode <-> SecurityEventType 연결
#### 설계 철학
- ExceptionCode
  - 무슨일이 일어났는지
- SecurityEventType
  - 그걸 어떻게 기록할지

#### 구현
```java
// ExceptionCode에 존재
public SecurityEventType toSecurityEventTypeOr(SecurityEventType fallback) {
    return securityEventType != null ? securityEventType : fallback;
}
```
#### EntryPoint 사용
```java
SecurityEventType eventType =
    exceptionCode.toSecurityEventTypeOr(SecurityEventType.AUTHENTICATION_FAILED);
```
#### 이 설계가 좋은 이유
- switch / if 제거
- 원인 중심 설계
- 확장 쉬움
- EntryPoint 코드 깔끔  
👉 **"원인이 의미를 결정한다"**

---
### 9️⃣ SecurityEventType + Severity 정리
- 이벤트 타입 = 정책
- Severity는 Type에 고정
```java
TOKEN_EXPIRED (WARNING)
REFRESH_REUSED (CRITICAL)
LOGIN_FAIL_THRESHOLD_EXCEEDED (CRITICAL)
```
👉 실수로 심각도 잘못 기록될 여지 ❌

---
### 1️⃣0️⃣ SecurityContext는 항상 완성된 상태라고 가정하면 안된다 
#### 먼저 선결론
SecurityContext가 완성되지 않은 상태로  
인가/예외 흐름에 진입할 수 있다  

**감사를 남기려면 request에 남겨둔 ClientContext가 필요하다**

#### SecurityContext는 언제 완성될까? 
##### 정상적인 JWT 요청 흐름
```text
Client Request
  ↓
ClientContextFilter        (환경 정보 수집)
  ↓
JwtAuthenticationFilter    (토큰 검증)
     ├─ 성공 → setAuthentication()
     └─ 실패 → 예외
  ↓
AuthorizationFilter
  ↓
Controller
```
##### 여기서 중요한 포인트
- SecurityContextHoler.getContext().setAuthentication(...)
  - 👉이 순간에만 SecurityContext가 완성됨
- 그 이전에는 : 
  - 비어있거나
  - AnonymousAuthenticationToken 이거나
  - 부분적으로만 채워진 상태일 수 있음

---
#### SecurityContext가 완성되지 않은 상태란 정확히 언제? 
##### ① 토큰이 없는 요청
- Authorization 헤더 없음
- JwtAuthenticationFilter는 그냥 doFilter()
- SecurityContext = **익명 사용자**  

👉이 상태로 **인가 필터까지는 감**

---
##### ② 토큰이 있지만 문제 있는 경우
예:
- 서명 깨짐
- 만료됨
- 형식 이상
```java
jwtTokenProvider.validateToken(token); // 여기서 예외
```
이 경우:
- ❌ setAuthentication() 호출 안됨 
- ❌ SecurityContext 미완성
- ⭕️ 바로 EntryPoint로 이동  
👉 **Controller는 아예 안탐**

---
##### ③ 토큰은 유효하지만 유저가 없음
(탈퇴 / 삭제된 유저)
```java
User user = userRepository.findById(userId)
    .orElseThrow(...)
```
이 경우도 : 
- 토큰은 통과
- but 인증 객체 생성 실패
- SecurityContext는 **완성되지 않음**
- 예외 흐름 진입

---
##### 그래서 왜 SecurityContext를 신뢰하지 말라고 했나?
AccessDeniedHandler
```java
Authentication authentication =
    SecurityContextHolder.getContext().getAuthentication();

if (authentication != null && authentication.isAuthenticated()
    && !(authentication instanceof AnonymousAuthenticationToken)) {
    ...
}
```
👉 이 방어 코드의 의미 : 
- SecurityContext가 **있을 수도 있고**
- **없을 수도 있고**
- 익명일 수도 있다  
즉, **인가 실패가 발생했다고 해서 항상 "정상 인증된 유저"가 있다는 보장은 없다**
  - 이게 오늘 가장 헷갈렸던 포인트

---
##### setAttribute(ClientContext)는 왜 중요?
**SecurityContext가 없거나 불완전해도 "요청 환경 정보"는 반드시 남기고 싶다**  

만약 ClientContext를 Filter에서 저장 안했다면?  
EntryPoint / DeniedHandler에서 :
- ❌IP 모름
- ❌User-Agent 모름
- ❌어떤 URI를 공격했는지 모름  
👉 **보안 감사로서 가치가 거의 없음**

---
##### 그래서 만든 것
```java
// ClientContextFilter
request.setAttribute(CLIENT_CONTEXT_KEY, clientContext);
```
이건 : 
- SecurityContext와 무관
- 인증 성공/실패와 무관
- **요청이 들어온 순간에 무조건 확보**  
👉 **"인증 실패해도 남아있는 정보"**

---
### 오해 정리⭐️
❌`setAttribute` 안하면 SecurityContext가 안 만들어진다
❌SecurityContext가 없으면 Spring Security가 잘못된 설계다  

⭕️Spring Security는 의도적으로 이렇게 설계됐다
- 익명 요청 허용
- 인증 실패도 필터 체인 흐름 유지
- 정책 위반 지점에서 처리 가능하게  
👉그 위에 audit 설계를 얹은 것 

⭕️`setAuthentication()`을 해야 SecurityContext가 의미있게 만들어진다  
`setAttribute()`는 SecurityContext랑 아무 상관 없다  

#### 🔹setAuthentication()
```java
SecurityContextHolder.getContext().setAuthentication(auth);
```
이게 의미하는 것:
- "이 요청은 **누가 보낸 요청인지** 확정됐다"
- Spring Security가 이 요청을 **인증된 요청으로 취급**
- 이후 : 
  - `@AuthenticationPrincipal`
  - `@PreAuthorize`
  - hasRole()
  - 인가 판단  
전부 여기서 나온다!  
👉 `setAuthentication()`을 해야 비로소 SecurityContext가 완성

#### 🔹setAttribute()
```java
request.setAttribute("clientContext", clientContext);
```
이건:
- 그냥 **request에 데이터 하나 붙인 것**
- 인증/인가와 무관
- SecurityContext를 만들지도, 바꾸지도 않음
- 누구든 쓸 수 있는 "요청 메모지"같은 존재  
👉 **보안 컨텍스트가 아니라, 감사용 메타데이터**

---
#### SecurityContext는 "객체는 항상 있다"
```java
SecurityContextHolder.getContext()
```
이건:
- 항상 객체를 돌려줌 
- null 아님
- ❗️문제는 내용물

---
#### SecurityContext 상태는 3가지가 있다 
##### ① 완성됨 (인증 성공)
```text
Authentication = UsernamePasswordAuthenticationToken
principal = CustomUserDetails
```
-> 우리가 원한느 정상 상태

---
##### ② 익명 상태 
```text
Authentication = AnonymousAuthenticationToken
```
- 토큰 없음
- permitAll 요청
- 인증 안됨

---
##### ③ 미완성 / 실패 상태
```text
Authentication = null
or
예외 발생으로 setAuthentication 전에 종료
```
- 토큰 검증 실패
- 서명 오류
- 만료
- 유저 없음  
👉 **이 상태로 EntryPoint/AccessDeniedHandler로 간다**

---
#### 정리 
❌`setAttribute` 안해서 SecurityContext가 없다  
⭕️`setAuthentication` 전에 실패하면 SecurityContext가 완성되지 않는다  

그리고 그 상황에서도 : 
- ❌userId는 없을 수 있지만 (EntryPoint에서 인증안된 경우)
- ⭕️IP/UA/URI 는 남겨야한다 (ClientContext DTO로 받아서)  

👉그래서 ClientContext를 Filter에서 미리 setAttribute

---
## Security Context 추가 공부 
### SecurityContext는 항상 "존재"하지만, 그 안이 "완성돼 있다"고는 절대 보장하지 않음 
| 상태    | SecurityContext 존재? | Authentication                      | 의미         | 
|-------|---------------------|-------------------------------------|------------|
| 인증 성공 | ⭕️                  | UsernamePasswordAuthenticationToken | 정상 사용자     |
| 익명 요청 | ⭕️                  | AnonymousAuthenticationToken        | 인증 안 함     |
| 인증 실패 | ⭕️                  | null                                | 인증 시도하다 실패 | 
| 필터 예외 | ⭕️                  | null                                | 컨텍스트 미완성   |
👉 **"SecurityContext는 항상 있지만, 내용은 상황마다 다르다"**

---
### 1️⃣ 왜 항상 존재하나? 
Spring Security는 요청이 들어오면 **미리 빈 SecurityContext를 만들어 둔다**
```java
SecurityContext context = SecurityContextHolder.getContext();
```
- 내부적으로 ThreadLocal에 묶임
- null을 리턴하지 않도록 설계됨
- 이후 필터들이 이 컨텍스트를 "채우거나 / 비워둔다"  

그래서 : 
❌SecurityContext가 없다  
⭕️SecurityContext 안에 Authentication이 없다

---
### 2️⃣ 상태 1 : 인증 성공 (완성 상태)
```java
SecurityContextHolder.getContext().setAuthentication(auth);
```
#### 내부 상태
```text
SecurityContext
 └── Authentication (UsernamePasswordAuthenticationToken)
       ├── principal = CustomUserDetails
       ├── authorities = [ROLE_USER]
       └── authenticated = true
```
#### 의미 
- 누가 요청했는지 확정
- 인가 판단 가능
- @PreAuthorize, hasRole() 정상 동작  
👉 **완성된 SecurityContext**

---
### 3️⃣ 상태 2: 익명 요청 (Anonymous)
토큰도 없고, permitAll 요청일 때 

#### 내부 상태 
```text
SecurityContext
 └── Authentication (AnonymousAuthenticationToken)
       ├── principal = "anonymousUser"
       ├── authorities = [ROLE_ANONYMOUS]
       └── authenticated = true (※ 주의)
```
⚠️authenticated = true 인 이유
-> "익명 사용자로 인증됨"이라는 의미 (혼동 포인트)

#### 의미 
- 로그인은 안함 
- 하지만 Spring Security는 이 요청을 **익명 사용자로 식별**
- 인가 단계에서 "ROLE_ANONYMOUS로 접근 가능한가?" 판단  
👉 **인증은 안 됐지만, 컨텍스트는 있음**

---
### 4️⃣ 상태 3 : 도메인 인증 실패 (비즈니스 판단 실패)
보안 판단 결과 실패 (인증 판단 단계에서 실패)  
**"인증 로직을 수행해봤는데, 정책/자격 증명 판단 결과 실패"**  
= AuthService(또는 인증 처리 유스케이스)에서 결론이 "NO"로 난 케이스 

#### 언제 발생?
- 요청이 **필터를 통과해서 컨트롤러/AuthService까지 도달**
- 사용자/자격(비밀번호, refreshToken 등)을 확인해 본 뒤
- **정책적으로 인증 실패**가 결정됨

예시 :
- 이메일/비밀번호 불일치 (로그인 API)
- 잠긴 계정이라 로그인 거부
- 로그인 실패 횟수 초과로 LOCK처리
- Refresh Token 재사용 감지(REFRESH_REUSED)로 강제 차단
- (케이스에 따라) refreshToken이 DB에 없거나 revoke 상태  
✅ 이건 "토큰 파싱 실패"가 아니라 **유스케이스 판단 실패**

#### 내부 상태 
- **이 요청이 "JWT 보호 리소스 접근"이 아니라 "/auth/login, /auth/refresh"같은 인증 API 호출**이라면  
  이 요청 자체가 "Authentication을 만들기 위한 요청"이므로   
  **SecurityContext를 채우지 않는 것이 정상**이다  
```text
SecurityContext
 └── Authentication = null
```
(로그인/리프레시 자체는 "인증을 만들기 위한 요청"이라서)
- 반면, "보호 리소스 접근"에서 **권한 부족(403)은 상태 3이 아니라 보통 AccessDeniedHandler 영역**

#### 감사(audit) 위치
- 이런 케이스는 AuthService에서 이미 분기/판단을 하니까 AuthService에서 남기는게 자연스러움  
  (로그인 실패 카운트 증가, LOCK 이벤트 기록 등)

#### 특징
- ❌ Spring Security 인증 실패 아님
- ❌ EntryPoint 관할 아님
- ⭕️ AuthService 책임
- ⭕️ SecurityEventService로 의미 있는 audit 기록

---
### 5️⃣ 상태 4 : 필터 체인 중간 실패 (인증 처리 도중 실패)
**"인증을 시도하려고 했는데, 처리 과정에서 예외로 탈락"**
= JwtAuthenticationFilter에서 validateToken/parse 단계에서 터지는 케이스 

##### 예시
- 토큰 만료
- 서명 오류
- 토큰 포맷 깨짐
- Bearer 뒤에 이상한 값
- parseBuilder().parseClaimsJws() 에서 예외

```java
jwtTokenProvider.validateToken(token); // 여기서 예외
```
#### SecurityContext 내부 상태
```text
SecurityContext
 └── Authentication = null
```
왜냐면 **setAuthentication() 하기 전에** 터져버리니까

#### 감사(Audit) 위치
- 이 케이스는 **컨트롤러/AuthService까지 못가므로**
- 만들어논 것 처럼 **EntryPoint에서 기록**하는게 딱 맞음

#### 결과
- JwtAuthenticationFilter 중간 탈락
- setAuthentication() 호출 안 됨
- 바로 EntryPoint로 점프 

👉 이게 "SecurityContext가 완성되지 않은 상태"의 대표 예

---
### 한 줄 요약
- 상태 3 (도메인 인증 실패) :  
  👉 "인증을 해봤고, 정책적으로 거부했다"  

- 상태 4 (필터 체인 실패) :  
  👉 "인증을 시작하기도 전에 기술적으로 실패했다"

---
### 6️⃣그래서 방어 코드가 필요한 이유 
```java
// JwtAccessDeniedHandler 에서 
Authentication authentication =
    SecurityContextHolder.getContext().getAuthentication();

if (authentication != null
    && authentication.isAuthenticated()
    && !(authentication instanceof AnonymousAuthenticationToken)) {
    // userId 추출
}
```
이건 : 
- SecurityContext가 **존재한다는 전제**
- 하지만 **내용은 신뢰하지 않는 태도**  
👉 보안 코드에서 좋은 습관  
👉**SecurityContext는 "주체" 정보**  
👉**ClientContext는 "환경" 정보**

---
### 8️⃣ 한 문장으로 다시 정리 
**SecurityContext는 항상 존재하지만,  
Authentication이 채워졌는지는 전혀 보장되지 않는다.**  
그래서 인증/인가 로직과 감사 로직을 분리하는게 맞다  