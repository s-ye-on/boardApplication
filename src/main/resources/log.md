# 로그 (logging)

> 이 글은 Spring Boot 기반 서버 애플리케이션에서  
> **운영과 보안을 고려해 로그를 어디에, 어떤 수준으로 남겨야 하는지**를 정리한 글입니다.

## 로그란? 
"프로그램이 실행되면서 남기는 기록"
```text
[언제] [어디서] [무슨 일이 있었는지]
```
### ❌로그가 없으면 
- 에러가 왜 났는지 모름
- 사용자 신고 -> 재현 불가
- 운영에서 거의 눈 감고 운전

### ✅로그가 있으면
- 에러 원인 추적
- 보안 사고 감지
- 사용자 행동 추적 (중요!)

## Spring Boot에서 로그는 기본으로 있다 
👉SLF4J + Logback

## 로그를 사용하기 위한 최소 설정
### 1️⃣클래스에 logger 선언 
```java
@Slf4j
@Service
public class AuthService {
```
(Lombok 사용 중이면 이게 제일 편함)

### 2️⃣ 로그 찍기
```java
log.info("비밀번호 변경 시도 userId={}", currentUserId);
log.warn("RefreshToken 재사용 감지 userId={}", userId);
log.warn("로그인 실패 email={}", email);
```

## 로그 레벨 ⭐️

| 레벨    | 언제 쓰나    |
|-------|----------|
| info  | 정상 흐름    |
| warn  | 의심스러운 상황 |
| error | 예외, 장애   | 
| debug | 개발 중만    |

### 프로젝트에 넣어야할 로그 ex
🔐 보안 관련
```java
log.info("로그인 성공 userId={}", userId);
log.warn("RefreshToken 재사용 감지 userId={}", userId);
log.info("비밀번호 변경 userId={}", userId);
log.info("로그아웃 userId={}", userId);
```
- 비밀번호 값은 절대 로그로 찍으면 안됨 

### 로그 레벨 선택 기준
#### 🟢info
정상적인 "중요 이벤트"
- 로그인 성공
- 비밀번호 변경
- 토큰 발급 

#### 🟡warn
이상하지만 시스템은 정상 동작
- 로그인 실패
- refresh token 재사용
- 권한 없는 접근

#### 🔴error
시스템이 기대한 대로 동작하지 않음
- DB 연결 실패
- 예외로 인해 요청 자체가 실패

## 로그는 "코드 흐름"이 아니라 "사건(Event)" 를 기록한다
"이 시점에 어떤 의미 있는 일이 발생했다"를 기록

## 어디에 찍는게 맞나? (핵심 기준 5가지)

### ✅ ① 상태가 바뀌는 지점 (가장 중요 ⭐️⭐️⭐️)
```java
log.info("비밀번호 변경 userId={}", userId);
log.info("RefreshToken revoke userId={}", userId);
```
- 데이터가 바뀜
- 보안적으로 중요한 지점
- 나중에 "누가 언제 뭘 했는지" 추적 가능  
👉 무조건 로그

### ✅ ② 인증 / 인가 경계
```java
log.info("로그인 성공 userId={}", userId);
log.warn("로그인 실패 email={}", email);
log.warn("권한 없는 접근 userId={} postId={}", userId, postId);
```
👉 보안 사고 분석용

### ✅ ③ 예외가 "의미를 가질 때"
```java
catch (TokenReuseException e) {
	log.warn("RefreshToken 재사용 감지 userId={}", userId);
	throw e;
}
```
❌모든 예외에 error 남기기  
✅시스템 장애급만 error

### ✅④ 외부 요청 / 외부 응답 경계 
- 로그인 요청
- 결제 요청
- 토큰 재발급 요청

```java
log.info("토큰 재발급 요청 userId={}", userId);
```

### ❌⑤ 절대 찍지 말아야 할 곳
```java
log.info("for문 시작");
log.info("if문 들어옴");
log.info("메서드 진입");
```
👉이건 디버그 로그지 운영 로그가 아님 (필요하면 debug로만)

### ❌절대 로그로 남기면 안 되는 정보
- 비밀번호 / Refresh Token / Access Token 
- 주민등록번호, 카드 정보 등 민감 정보 
- 인증 헤더 전체 값 

### 로그는 어느 계층에 남겨야 할까? 
#### ✅Controller
- 요청이 들어왔다는 사실
- 누가 요청했는지

#### ✅Service ⭐️⭐️⭐️
- 로그의 중심
- 비즈니스 이벤트 기록

#### ❌Repository
- 웬만하면 로그 안 찍음 (DB 쿼리는 프레임워크가 알아서)

## 서비스 코드에 로그는 보통 이렇게 
```java
@Transactional
public void updatePassword(...) {
	log.info("비밀번호 변경 시도 userId={}", userId);

	...
	
	refreshTokenRepository.revokeAllByUser(user);

	log.info("비밀번호 변경 완료 userId={}", userId);
}
```
👉시작 / 결과만 남김  
중간 과정 ❌
