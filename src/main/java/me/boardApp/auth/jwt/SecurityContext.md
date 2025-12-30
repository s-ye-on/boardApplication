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
authentication = new AnonymousAuthenticationToken(...)
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



