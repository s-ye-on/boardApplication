# 보안 이벤트 로깅 (SecurityEvent)
# 인증·인가 이벤트 로깅 설계하기 

## 지금 log.warn이 있는데도, 왜 SecurityEvent가 필요해? 
log.war / log.error = "개발자 디버깅/운영 로그"
- 서버에서 무슨 일이 있었는지 **흔적을 남김**
- 파일/콘솔/로그 수집툴(ELK, CloudWatch 등)로 흘러감
- 장점 : 구현 쉬움, 즉시 확인 가능
- 단점 : 
  - **유저별로 사건을 모아서 보기 어려움**
  - "최근 7일간 refresh 재사용 감지된 계정" 같은 **조회/통계가 어려움**
  - 로그 보관기간/포맷이 운영 설정에 의존 

## SecurityEvent(DB 저장) = "보안 감사(Audit) 데이터"
- "보안 사건"을 **조회/분석/증거**로 남김
- 관리자 페이지로 바로 보여줄 수 있음
- 예시로 가능한 것들 : 
  - 특정 userId의 최근 보안 이벤트 타임라인
  - IP별 로그인 실패 횟수 랭킹 
  - REFRESH_REUSED 발생 시 자동 계정 잠금 근거 남기기  

✅ 결론 : 
**로그는 '관찰'**, 이벤트 테이블은 **기록/감사/조회**  
둘은 같이 가는게 정석, 프로젝트가 보안까지 다룬다는 증거가 됨 

---

## 그럼 둘 다 남겨야 하나? 
추천 : 
- **보안적으로 중요한 사건** : DB(SecurityEvent) + log.info/warn 둘 다 
- **디버깅용 상세 스택트레이스** : log.error(..., e) 만  

즉,  
- "Refresh 재사용 감지 " = DB에도 남기고 로그도 남김 
- "그냥 파라미터 검증 실패" = 굳이 DB까지는 선택  

--- 

## 코드 설명 

### SecurityEvent 엔티티
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityEvent {
```
- `@Entity` : JPA가 DB 테이블로 관리하는 클래스
- `@Getter` :  필드 getter 자동 생성
- `@NoArgsConstructor(PROTECTED)` : JPA가 엔티티 만들 때 기본 생성자가 필요해서 넣는거고, 외부에서  
  막 생성하지 못하게 PROTECTED로 막아둔 것 

```java
@Id @GeneratedValue
private Long id;
```
- id : PK(기본키)
- `@GeneratedValue` : DB가 자동으로 증가시켜서 넣어줌  

```java
@Enumerated(EnumType.STRING)
private SecurityEventType type;
```
- type : 이벤트 종류 (LOGIN_SUCCESS, REFRESH_REUSED 등)
- `EnumType.STRING` : enum 이름 그대로 저장.  
  (ORDINAL 쓰면 enum 순서 바뀌면 데이터 망가짐 -> STRING 추천)

```java
private Long userId; // null 가능 (인증 전 실패)
```
- 인증이 되기 전(로그인 실패, 토큰 위조 등)에는 userId를 모를 수 있음
- 그래서 null 허용 **중요**  

```java
private String ipAddress;
private String userAgent;
```
- 누가 어디서 요청했는지 추적용
- User-Agent는 브라우저/앱 정보  

```java
private String message;
```
- "Refresh Token 재사용 감지" 같은 사람이 읽을 설명
- 나중에 관리자 페이지/로그 화면에 그대로 쓰기 좋음 

---

### 팩토리 메서드 (생성 편의)
```java
public static SecurityEvent of(
    SecurityEventType type,
    Long userId,
    String ip,
    String agent,
    String message
) {
```
- 생성 규칙을 한 곳에 모아두는 방식
- "new SecurityEvent() 여기저기서 직접 만들기" 를 막고, 데이터 누락을 예방  

```java
SecurityEvent event = new SecurityEvent();
event.type = type;
event.userId = userId;
event.ipAddress = ip;
event.userAgent = agent;
event.createdAt = LocalDateTime.now();
event.message = message;
return event;
```
- 실제 객체를 만들고 필드를 채우는 부분
- createdAt을 여기서 찍어두면 "기록 생성 시점"이 항상 일관됨 

---

## SecurityEventService
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {
```
- `@Service` : 스프링 빈으로 등록 (비즈니스 서비스)
- `@RequiredArgsConstructor` : final 필드 생성자 자동 생성
- `@Slf4j` : log.info() 같은 로거 자동 생성
  
```java
private final SecurityEventRepository repository;
```
- DB 저장 담당 리포지터리 주입  

```java
public void log(
    SecurityEventType type,
    Long userId,
    HttpServletRequest request,
    String message
) {
```
- "이벤트 남기기"의 표준 진입점
- request를 받아서 ip/user-agent를 추출하기 위함  

```java
SecurityEvent event = SecurityEvent.of(
    type,
    userId,
    request.getRemoteAddr(),
    request.getHeader("User-Agent"),
    message
);
```
- request에서 필요한 정보를 뽑아 이벤트로 조립  

```java
repository.save(event);
```
- DB 저장  (이 한 줄이 "감사/조회 가능"을 만들어줌)  

```java
log.info("[SECURITY] {} - userId={}", type, userId);
```
- DB 저장과 별개로 운영 로그도 남김
- 사건이 터졌을 때 로그로도 바로 추적 가능  

---

## 내 프로젝트에서 "어디에서" 찍어야 의미가 있을까?
1. AuthService.login() 성공/실패
2. AuthService.refresh() 에서 REFRESH_REUSED 감지 지점
3. JwtAccessDeniedHandler에서 403 발생 시점 

---

---

## 지금 코드가 HttpServletRequest 없이도 잘 동작하는 이유 
### 지금 AuthService의 책임
- 이메일/비밀번호 검증
- JWT 발급
- RefreshToken 관리
- 예외 던지기  

즉, **비즈니스로직 + 보안 정책 판단**  
만 하고 있음  

### 인증/인가 흐름 관점
- JWT 검증 -> JwtAuthenticationFilter
- 인증 실패 -> AuthenticationEntryPoint  
- 인가 실패 -> AccessDeniedHandler
- 컨트롤러 접근 -> OK  

👉 이 흐름에는 **HttpServletRequest를 AuthService가 알 필요가 전혀 없다**  

---

## 그럼 HttpServletRequest는 언제 필요해질까? 
### 답 : "보안 이벤트를 기록하고 싶을 때"
- 지금 목표 : 인증 보안 이벤트 로깅에 만들어보자  

## 보안 이벤트 로깅에서 중요한 정보들  
로그인/토큰/보안 이벤트를 **나중에 다시 분석하려면,**  
단순히 "실패했다" 만으로는 부족  

보통 이런 정보들이 필요 👇  

| 정보           | 왜 필요할까?        |
|--------------|----------------|
| IP 주소        | 공격 IP 추적       |
| User-Agent   | 브라우저/기기 구분     |
| 요청 URI       | 어떤 엔드포인트를 노렸는지 |
| 시각           | 타임라인 분석        |
| userId (있다면) | 계정 단위 추적       |

이 정보들의 **출처는 전부 HttpServletRequest**  

## log.warn은 있는데, 굳이 Request까지 필요한가? 
지금 코드
```java
log.warn("잠긴 계정 로그인 시도 userEmail = {}", request.email());
```
이 로그는 ✔운영 로그로는 충분  
하지만 ❌보안 감사(Audit) 용으로는 부족

### log.warn의 한계
- 로그 파일에 흩어짐
- IP / User-Agent 없음
- 사용자별 / 기간별 조회 어려움
- 관리자 화면에서 보여주기 힘듬 

## HttpServletRequest를 쓰는 순간 생기는 차이 
예를 들어 로그인 실패 시 

### 현재 
```text
[WARN] 로그인 실패 userEmail=xxx
```

### 보인 이벤트 로깅 사용 시 
```text
LOGIN_FAIL
userId: null
email: xxx
ip: 203.0.113.10
userAgent: Chrome / Mac
time: 2025-01-10 22:31
```
👉 **"이건 공격인가?"** 를 판단할 수 있는 정보가 생김

## 그런데 ㅗ애 Service에서 받아야할까? 
### ❌Service가 직접 HttpServletRequest를 꺼내면  
```java
RequestContextHolder.getRequestAttributes() ❌
```
- 테스트 지옥
- 웹 계층에 종속
- 나중에 배치/메시지 처리 불가

### ✅Controller -> Service로 전달
```java
@PostMapping("/login")
public ResponseEntity<?> login(
    @RequestBody LoginRequest request,
    HttpServletRequest httpRequest
) {
    authService.login(request, httpRequest);
}
```
이렇게 하면 : 
- 웹 계층 책임 유지
- Service는 "필요한 정보만 전달받음"
- 구조적으로 깨끗함

## 정리
### 지금 상태 (jwt-auth 브랜치) 
- HttpServletRequest 없어도 완전 정상
- 인증/인가 구조 굿

### HttpServletRequest를 받는 이유 (feature/security-event-logging 브랜치)
- ⭕️보안 이벤트 로깅을 제대로 하기 위해서

## 어떻게 바뀌나? 
### 지금 방식 
```java
log.warn("잠긴 계정 로그인 시도 userEmail = {}", request.email());
```

### 바꿔볼 방식 (예시)
```java
securityEventLogger.log(
    SecurityEventType.LOGIN_FAILED_LOCKED,
    user.getId(),
    request.email()
);
```

## Controller에서 Service로 request(HttpServletRequest) 자체를 넘기지 말자


## 서비스에서 직접 받게 하지 않는게 정확히 무엇? 
Service는 '웹 요청 객체(HttpServletRequest)'를 몰라야 한다는 뜻  

Service는 원래 "비즈니스 규칙/유스케이스"계층이라서  
HTTP가 아닌 다른 вход(스케줄러/메시지/테스트/CLI)에서도 호출될 수 있어야 깔끔

## 생각 
### 무엇을 입력으로?
✅ HttpServletRequest 그 자체 말고 ClientContext(ip, userAgent, uri) 같은 DTO + (userId/email) + eventType

### AuthService 책임? 
✅ 이벤트 발생 판단 + eventType 결정 + (필요한 최소 정보 전달)

### request는 어디서 넘기나? 
✅ request는 Controller/Filter에서만 다루고  
Service에는 추출된 DTO만 넘기기 

SecurityEventService.log()
```java
public void log(
		SecurityEventType type,
		Long userId,
		HttpServletRequest request,
		String message
	) {
		SecurityEvent event = SecurityEvent.of(
			type,
			userId,
			request.getRemoteAddr(),
			request.getHeader("User-Agent"),
			message
		);

		repository.save(event);

		log.info("[SECURITY] {} - userId={}", type, userId);
	}
```
처음 설명 들을 때는 이해를 쉽게 하기 위해 HttpServletRequest를 직접 받았지만 이런 설계는 웹 계층과 서비스 계층이 강하게 묶이게 만든다  
HttpServletRequest를 직접 서비스로 연결(매개변수로 서비스에게 넘기지)말고 DTO를 통해 필요한 정보만 넘기도록 하자  
계층 구분! 

## ClientContext를 어디서 만들어야 할까?
ClientContext는 HttpServletRequest에서 로깅에 필요한 정보들만 추출한 dto  

### 어디에 만들어야할까?
#### ❌ AuthService에 ? 
```java
String ip = request.getRemoteAddr(); // ❌
```
- 다시 HttpServletRequest 의존 발생 -> 서비스 계층이 웹 계층에 의존

#### Controller에서 직접 생성 ❌ (비추천)
```java
@PostMapping("/login")
public ResponseEntity<?> login(
    @RequestBody LoginRequest request,
    HttpServletRequest httpRequest
) {
    ClientContext context = new ClientContext(
        httpRequest.getRemoteAddr(),
        httpRequest.getHeader("User-Agent"),
        httpRequest.getRequestURI()
    );

    authService.login(request, context);
}
```
문제점 : 
- 모든 컨트롤러에 반복 코드
- 컨트롤러가 "요청 파싱 + 비즈니스 흐름 + 로깅 준비" 까지 떠안음 
- 컨트롤러가 더러워짐 

#### Filter에서 생성해서 attribute로 전달 ⭕️(가장 추천)
Filter에서 ClientContext 생성  
```java
ClientContext context = new ClientContext(
    request.getRemoteAddr(),
    request.getHeader("User-Agent"),
    request.getRequestURI()
);

request.setAttribute("clientContext", context);
```

필요한 곳에서 꺼내 쓰기 
```java
ClientContext context =
    (ClientContext) request.getAttribute("clientContext");
```
장점 : 
- 컨트롤러 깨끗
- 요청 정보 수집은 "입구(Filter)"에서 한 번만
- 인증 / 인가 / 로깅 어디서든 재사용 가능
- Service는 DTO만 받음  
  
👉책임 분리 완벽  

#### 전용 Resolver / Helper 사용 ⭕️ (고급)
예를 들면 :  
```java
@Component
public class ClientContextResolver {

    public ClientContext resolve(HttpServletRequest request) {
        return new ClientContext(
            request.getRemoteAddr(),
            request.getHeader("User-Agent"),
            request.getRequestURI()
        );
    }
}
```
- Filter나 Controller에서 이 Resolver만 호출
- 테스트/확장성 최고  

👉2번 해보고 이걸로도 만들어보자  


### 지금 단계에서 이해해야할 것 
- DTO는 Controller가 만드는 것이 아님
- 요청 맥락은 Filter에서 한 번만 만든다
- Service는 받은 정보만 사용한다  
  
레이어 설계의 핵심 원칙  

## Filter에서 DTO를 만들었는데 왜 securityEventService.record()가 또 필요한가? 
"CLientContext를 이미 Filter에서 만들었는데, 왜 굳이 Service를 하나 더 두고 record를 호출하지?"

이유 :  
Filter는 재료준비  
Service는 의미 있는 행위 기록  

### Filter의 역할 (ClientContextFilter)
Filter는 아무 판단도 하지 않는다 
```java
ClientContext clientContext = new ClientContext(
    request.getRemoteAddr(),
    request.getHeader("User-Agent"),
    request.getRequestURI(),
    request.getMethod()
);

request.setAttribute("clientContext", clientContext);
```
"이 요청이 어떤 환경에서 왔는지 **있는 그대로 수집**"  
👉Filter는 관찰자 

--- 

### Service의 역할 (SecurityEventService)


## ClientContext.extractIp(HttpServletRequest request)
```java
private static String extractIp(HttpServletRequest request) {
    // 프록시/로드밸런서 환경 고려
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
        return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
}
```
왜 이런 코드가 필요한가? 
- requeset.getRemoteAddr()는 **항상 "진짜 사용자 IP"가 아니다**

### 현실의 서버 구조 
실제 서비스 구조는 보통 이렇게 생김 : 
```java
[사용자 브라우저]
        ↓
[프록시 / 로드밸런서 / CDN]
        ↓
[Spring Boot 서버]
```
이 경우: 
- request.getRemoteAddr() : 👉**프록시 서버 IP**
- 진짜 사용자 IP : 👉**HTTP 헤더에 따로 실려서 옴**  

그게 바로 X-Forwarded-For

---

### X-Forwarded-For 헤더란? 
```http request
X-Forwarded-For: 203.0.113.10, 10.0.0.1, 10.0.0.2
```
의미: 
- 맨 앞 : 실제 사용자 IP
- 뒤쪽 : 거쳐온 프록시들  

그래서 이 코드가 필요 👇
```java
forwarded.split(",")[0].trim();
```
- 실제 사용자 IP만 뽑기
- 프록시 환경에서도 로그 신뢰도 유지 

### 정리 
```java
if (X-Forwarded-For 존재)
    → 진짜 사용자 IP
else
    → 직접 접속한 IP (개발환경 / 로컬)
```
---

# 12월 22일
## ClientContext 왜 만들었나? 
### 문제 
HttpServletRequest를 서비스마다 넘기면 : 
- 서비스 웹 기술에 의존
- 테스트/배치에서 깨짐
- 책임 분리 붕괴

### 해결
**요청에서 필요한 정보만 뽑아서 DTO로 만든다 
```java
public record ClientContext(
	String ipAddress,
	String userAgent,
	String requestUri,
	String httpMethod
)
```
이 DTO 하나로 : 
- 웹 요청
- 필터
- 서비스 
- 로그  
를 전부 연결 가능 

---

### ClientContext.from(request) - 여기에서 뭘 하나? 
```java
public static ClientContext from(HttpServletRequest request) {
	return new ClientContext(
		extractIp(request),
		request.getHeader("User-Agent"),
		request.getRequestURI(),
		request.getMethod()
	);
}
```
이 메서드의 의미
- HTTP 파싱 책임을 DTO 내부에 숨김 
- AuthService는 **파싱 방법 몰라도 됨**  
👉 **"어떻게 뽑는지"는 ClientContext 책임**

---

### extractIp() - 왜 이렇게 복잡해 보일까? 
```java
String forwarded = request.getHeader("X-Forwarded-For");
```
#### 이유
실부에선 보통 구조가 이렇게 됨 👇
```text
Client → LoadBalancer → Proxy → Server
```
- request.getRemoteAddr() 👉 프록시 IP  
- 실제 사용자 IP 👉 X-Forwarded-For
```java
return forwarded.split(",")[0].trim();
```
👉 여러 프록시를 거칠 수 있어서  
**"첫 번째 IP = 진짜 클라이언트"**

--- 

### ClientContext.system() - 왜 필요했나? 
#### ❓"요청이 없을 수도 있어?"
있다. 예를 들면 : 
- 배치 작업
- 관리자 강제 로그아웃
- 테스트 코드
- 내부 이벤트  

그래서 만든게 👇
```java
public static ClientContext system(){
	return new ClientContext("SYSTEM", "SYSTEM", "-", "-");
}
```
👉 NPE 방지 + 설계 안정성

---

## SecurityEventService 
```java
public void record(
	SecurityEventType type,
	Long userId,
	ClientContext context,
	String message
)
```
### 이 메서드의 역할
- 인증 로직 ❌
- 판단 ❌
- ✅ "보안 이벤트를 기록하는 책임"만 

```java
SecurityEvent event = SecurityEvent.of(
	type,
	userId,
	context.ipAddress(),
	context.userAgent(),
	message
);
```
- 어떤 이벤트인지
- 누가했는지 
- 어디서 했는지 
- 메시지는 뭔지  

**형태를 통일해서 DB에 저장**

---

## AuthService는 왜 이렇게 쓰나? 
```java
securityEventService.record(
        SecurityEventType.LOGIN_SUCCESS,
        user.getId(),
        context,
        SuccessMessage.LOGIN_SUCCESS.getMessage()
);
```
### AuthService의 책임
- "이건 로그인 성공이다"
- "이건 계정 잠금이다"  
👉 **무슨 이벤트인지만 결정**

### 기록 방법은? 
👉SecurityEventService가 전담  

서비스 분리  

