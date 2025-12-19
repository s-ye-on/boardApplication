# JWT와 Spring Security 인증 구조 한 번에 이해하기 
- 세션 기반 인증과 무엇이 어떻게 다른가 

## 이 글의 목표
이 글은 **JWT를 구현하기 전에 반드시 이해해야 할 전체 그림**을 정리한다
- JWT가 무엇인지
- 세션 기반 인증과 어떤 차이가 있는지 
- Spring Security와 JWT의 역할이 어떻게 나뉘는지 
- JWT 인증이 **Spring Security 내부에서 어떤 흐름으로 동작하는지**  

👉**구현 코드가 아니라 구조와 개념 중심**이다 
👉실제 구현은 다음 글에서 다룬다 
---
## JWT란? 
JWT(Json Web Token)는 **사용자의 인증 정보를 토큰에 담아 클라이언트가 직접 들고 다니는 인증 방식**이다

### 핵심 아이디어 
- 사용자가 로그인하면 서버가 JWT를 발급
- 클라이언트(브라우저/앱)가 토큰을 저장
- 이후 요청마다 JWT를 HTTP Header에 포함
- 서버는 **세션을 저장하지 않고,** 토큰이 유효한지만 검증  

즉, **인증 상태를 서버가 아니라 클라이언트가 보관**하는 방식이다

---

## 세션 기반 인증 vs JWT 인증 

### 세션 기반 인증(기본 Spring Security)
- 로그인 성공 시 서버가 **세션 생성**
- JSESSIONID 쿠키 발급 
- 이후 요청마다 쿠키로 사용자 식별 
- 인증 정보는 **서버 메모리/Redis 등에 저장**  
- **"세션은 서버에 있고, 쿠키는 세션을 찾기 위한 열쇠다"**

👉서버가 **상태(state)를 가진다**

### 세션 기반 인증의 한계 
❗️한계 1 : 서버 확장(Scale-out)에 취약  
**세션은 특정 서버에 종속된다** -> 세션 불일치 (Session Inconsistency) 문제  
"Sticky session"이나 "Spring Session + Redis"로 해결 가능

❗️한계 2 : 서버 리소스 부담 증가 
- 세션은 서버 메모리를 계속 점유 
- 대규모 트래픽에서 메모리/관리 비용 증가  

❗️한계 3 : 상태 기반(Stateful)구조 
- 서버가 **사용자 상태를 기억해야 함**
- 무상태(stateless)한 API 설계에 부적합 

---

## JWT 기반 인증
- 로그인 성공 시 서버가 **JWT 발급**
- 클라이언트가 토큰을 저장
- 요청마다 Authorization: Bearer <token> 전송
- 서버는 토큰을 검증만 하고 **세션 저장❌**  

👉서버가 무상태(stateless)로 동작한다  

---

### 차이 한 줄 요약
세션은 “서버가 기억”하고,  
JWT는 “클라이언트가 들고 다닌다”  

---
## JWT 인증의 장단점 
### ✅장점 
- 서버에 세션 저장소 불필요
- 서버 확장(Scale-out)에 유리 
- 모바일 / SPA 환경에 적합 
- 인증 처리 흐름이 **요청 단위로 명확**

### ❌단점 
- 토큰 탈취 시 위험
- 발급된 토큰은 즉시 무효화가 어려움
- 토큰 크기가 커 네트워크 비용 증가  

👉이 단점을 보완하기 위해  
**Access Token + Refresh Token 구조**를 사용한다.  
Access Token은 짧게, Refresh Token은 서버 저장(DB/Redis)으로 관리하는 방식을 많이 쓴다  
(이 내용은 4편에서 다룬다)

---

## Spring security는 "인증 방식"이 아니다
이 부분은 꼭 짚고 넘어가자 
### Spring Security
- 인증 / 인가를 처리하는 **보안 프레임워크**
- 필터 체인, SecurityContext, 권한 체크 제공

### JWT
- 인증을 위한 **수단(방식)** 중 하나  

즉,  
- Spring Security = **틀**
- JWT = **그 틀 안에서 사용하는 전략**  
  
Spring Security는 다음과 같은 인증 방식들을 모두 처리할 수 있다 
- Form Login
- 세션 기반 인증
- JWT 인증
- OAuth2

---

## Spring Security + JWT가 만났을 때의 전체 흐름 
JWT 기반 인증에서도  
**Spring Security의 핵심 구조는 그대로 유지**된다 

### 전체 요청 흐름 
```java
클라이언트 요청
    ↓
Spring Security Filter Chain
    ↓
JWT 인증 필터에서 토큰 검증
    ↓
SecurityContextHolder에 인증 정보 저장
    ↓
Controller / Service 로직 실행
```
👉차이점은 딱 하나다 
- 세션 기반 : 로그인 시 한 번 인증
- JWT 기반 : **요청마다 토큰으로 인증**  

내 프로젝트에서는 **Authorization: Bearer 토큰을 매 요청마다 검사하는 필터(JwtAuthenticationFilter)를 두고,  
검증 성공 시 **SecurityContextHoler에 Authentication을 세팅**해서 `@AuthetnticationPrincipal`을 그대로 사용했다.  

---

## JWT 기반 인증의 실제 동작 흐름 

### 1️⃣로그인 
- 클라이언트가 이메일/비밀번호 전송
- 서버에서 사용자 검증
- 검증 성공 -> JWT 발급 
```http request
POST /auth/login
```
---

### 2️⃣토큰 저장
- 클라이언트가 JWT를 저장
  - localStorage / sessionStorage / cookie 등

---

### 3️⃣인증이 필요한 요청 
```http request
Authorization: Bearer <JWT>
```
---

### 4️⃣Spring Security 필터 체인 동작 
- JWT 필터가 토큰을 검증
- 토큰에서 사용자 정보 추출
- Authentication 객체 생성
- SecurityContext에 저장

---

### 5️⃣컨트롤러 접근 
- 이후 로직에서는 기존과 동일하게 사용 가능 
```java
@AuthenticationPrincipal CustomUserDetails user
```
👉컨트롤러 / 서비스는 **SecurityContext를 쓰는 방식이 동일해서** 거의 바뀌지 않는다

---
## 📚핵심 정리 
- JWT는 인증 정보를 토큰에 담아 클라이언트가 보관하는 방식 
- Spring Security는 인증/인가를 처리하는 프레임워크
- JWT 인증에서도 Spring Security의 구조는 그대로 유지된다 
- 차이는 **세션 대신 토큰으로 인증한다는 점**
- 인증 결과는 항상 SecurityContextHolder에 저장된다

---

## 다음 글 예고 
다음 글에서는  
**JWT 인증을 실제로 어떻게 구현하는지**를 다룬다.  
- JwtTokenProvider는 왜 필요한가
- JwtAuthenticationFilter는 어떤 역할을 하는가
- 왜 OncePerRequestFilter를 사용하는가 
- SecurityConfig에서 무엇이 바뀌는가  

👉[2편] Spring Security + JWT 인증 구현하기 (Filter & Token)