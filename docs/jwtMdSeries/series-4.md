# Refresh Token 설계와 보안 정책 선택
## Rotation · Reuse Detection · 다중 로그인 전략 (JWT 4편)

---

## 이 글의 목표
앞선 글들에서 우리는  
- JWT 인증 구조
- Token / Filter / Config 구현
- 401 / 403 보안 예외 처리  

까지 완성했다  

이제 마지막으로 **Refresh Token은 어떻게 관리해야 안전한가?** 에 대해 알아보자  

이 글에서는 :  
- 왜 Refresh Token이 필요한지
- 단순 구현의 한계
- Refresh Token Reuse Detection (재사용 감지)
- 다중 로그인 정책에 따른 설계 선택지  

를 실제 게시판 프로젝트 코드 기준으로 정리하겠다 

---

## 왜 Refresh Token이 필요할까?

### Access Token의 문제 
Access Token은 :  
- 짧은 만료 시간 (보통 15분~ 1시간)
- 탈취 시 피해를 줄이기 위한 설계  

👉하지만 단점이 존재  

❌만료될 때마다 다시 로그인해야 한다 
❌UX가 매우 나빠진다

---

### 해결책 : Refresh Token

- Access Token 재발급 전용 토큰 
- 더 긴 만료 시간
- 서버(DB)에 저장해서 관리 가능 

```text
Access Token 만료
 → Refresh Token으로 재발급 요청
 → 새 Access Token 발급
```
---

## Refresh Token을 서버에 저장하는 이유 
JWT는 원래 Stateless 이지만  
**Refresh Token은 Stateless 구조에서 의도적으로 예외 취급된다**  

이유 : 
- 로그아웃
- 강제 만료
- 탈취 감지
- 재사용 탐지  

👉이건 서버가 상태를 알아야만 가능하다  

그래서 보통 다음과 같이 분리한다  
- Access Token : Stateless
- Refresh Token : Stateful(DB / Redis)

---

## 단순한 Refresh Token 구현의 문제
❌나쁜 예 : 덮어 쓰기 방식
```text
로그인
 → Refresh Token A 저장

재로그인
 → Refresh Token B로 덮어씀
```
이 방식의 치명적인 문제가 있다 : 
- **재사용 감지 불가능**
- 탈취돼도 모른다  

👉공격자가 옛날 토큰을 써도 "정상 요청"처럼 보인다 

--- 

## Refresh Token Rotation
Rotation은 재사용 감지를 가능하게 만들기 위한 전제 조건이다  

### 개념
Refresh Token을 사용할 때마다 : 
- 기존 토큰을 폐기 (REVOKED)
- 새 토큰을 발급


```text
Refresh Token A 사용
 → A는 REVOKED
 → Refresh Token B 발급
```
---

### 코드 구현 
```java
refreshToken.revoke();

String newRefreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());
LocalDateTime newExpiry = jwtTokenProvider.calculateRefreshExpiry();

RefreshToken newRefreshToken =
    RefreshToken.create(user, newRefreshTokenValue, newExpiry);

refreshTokenRepository.save(refreshToken);
refreshTokenRepository.save(newRefreshToken);
```
✔ 이전 토큰은 무조건 무효  
✔ 새 토큰만 ACTIVE  

---

### Refresh Token ReUse Detection (재사용 감지)

#### 핵심 아이디어 
이미 **REVOKED 된 Refresh Token이 다시 사용되면  
그건 거의 확실한 공격**

---

코드 핵심 로직 
```java
		if (refreshToken.getStatus() == RefreshToken.Status.REVOKED) {
			user.lock();
			log.warn("Refresh Token 재사용 감지 -> 계정 잠금 userId ={}", refreshToken.getUser().getId());

			// refresh token 전부 revoke
			refreshTokenRepository.revokeAllByUser(refreshToken.getUser());

			throw new AuthorizationException(ExceptionCode.REFRESH_REUSED);
		}
```
여기서 의미하는 것 : 
- 토큰 탈취 가능성 매우 높음
- 계정 잠금
- 모든 Refresh Token 무효화  

👉**보안 최우선 정책**

---

## 그런데 정말 공격만 있을까?
여기서 현실적인 문제가 등장

---

### ⚠️다중 로그인 환경의 문제 

#### 시나리오 
```text
기기 A 로그인 → Refresh Token A
기기 B 로그인 → Refresh Token B
→ A는 REVOKED
```
이후 : 
- 사용자가 다시 A기기에서 refresh 요청
- A는 이미 REVOKED
-❗️공격이 아님에도 불구하고 토큰 탈취로 오판되는 상황    

👉이 부분에서 **정책 선택의 문제**

---

### Refresh Token 정책 선택지 3가지 
#### ① 단일 로그인 정책 (현재 내 구현)
- 마지막 로그인만 유효
- 이전 토큰 전부 REVOKED
- 보안은 강력하지만 UX는 희생된다  

✔ 개인 프로젝트 / 관리자 시스템에 적합 

--- 
#### ② 기기 별 Refresh Token (대부분의 서비스에서 선택하는 방식)
```text
User
 ├─ Session A (deviceId=A)
 ├─ Session B (deviceId=B)
```
- Refresh Token = 세션 
- 기기 별 독립 관리
- 진짜 재사용만 공격으로 판단  

✔ 대부분의 서비스가 선택한 방식 

--- 

#### ③ Refresh Token Family (보안 최상)
이 부분은 아직 깊게 배우지 않으니 간단히 소개만 하겠다 
- 토큰이 체인처럼 이어짐 
- 분기 발생 시 즉시 공격 판정  

✔ 금융 / OAuth2 / Google 계열  
❌구현 난이도 높음 

---

### 다시 내 프로젝트의 정책  
이 프로젝트에서는 보안 개념을 정확히 이해하는 것이 목적이므로,  
UX보다 보안을 우선하는 단일 로그인 정책을 선택했다

이미 주석으로 :
- 정책 한계
- 대안 3가지 
- 실무 선택지  
에 대해 정리해놨으니 이것들도 만들어보고 정리해보겠다 

---
## 📚마무리 정리
- Refresh Token은 **보안의 핵심**
- 단순 저장/덮어쓰기는 위험
- Rotation은 필수
- Reuse Detection은 선택이 아닌 보안 전략
- 다중 로그인 정책은 서비스 성격에 따라 결정  

JWT 보안의 핵심은  
"토큰을 쓰는 것"이 아니라  
"토큰을 어떻게 관리하느냐"에 있다  

---

### 시리즈 전체 요약 
1️⃣JWT와 Spring Security 구조 이해  
2️⃣Token / Filter / Config 구현
3️⃣401 / 403 보안 예외 처리 
4️⃣Refresh Token 보안 설계와 정책 선택 
