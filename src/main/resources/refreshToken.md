# Refresh Token

## 1. Refresh Token 흐름 간단 설계 
우리가 만들고 싶은 흐름 : 
1. /auth/login 성공 시 : 
   - Access Token (짧은 만료)
   - Refresh Token (긴 만료)
   - 둘 다 발급 
2. Refresh Token은 DB에 저장 :
   - userId, token 값, 만료 시각, 생성 시각 정도
3. 클라이언트는 : 
   - Access Token : Authorization: Bearer ... 로 계속 사용 
   - Refresh Token : 
     - 지금은 간단하게 응답 JSON으로 주고, 클라이언트가 보관하도록 두자 (추후 httpOnly 쿠키로 옮겨도 됨)
4. /auth/refresh 엔드포인트 : 
   - Body/헤더로 refresh 토큰을 보내면,
   - DB에서 유효한지 확인 (존재 + 만료 안 됨)

## 2. Refresh Token 엔티티 설계 
간단하게 이정도 : 
- id (PK, Long)
- userId (또는 @ManyToOne User user)
- token (실제 refresh 토큰 문자열, unique)
- expiryDate (만료 시각)
- createdDate (선택 - BaseEntity에 이미 존재)

## 3. JwtTokenProvider에 Refresh Token 생성 메서드 추가 
`generateRefreshToken(userId, email, role)`  
application.yml에 refresh 만료도 추가
```yaml
app:
  jwt:
    secret: very-secret-jwt-key-change-me-please-very-long
    expiration-seconds: 3600        # access token: 1시간
    refresh-expiration-seconds: 604800 # refresh token: 7일 (예시)
```
```java
//Refresh Token - 보통 subject만 써도 충분함
	public String generateRefreshToken(Long userId){
		long now = System.currentTimeMillis();
		Date validity = new Date(now + refreshTokenValidityInSeconds * 1000);
		
		return Jwts.builder()
			.setSubject(String.valueOf(userId))
			.setIssuedAt(new Date(now))
			.setExpiration(validity)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}
```
- validateToken, getUserId 등은 Access/Refresh 둘 다 쓸 수 있게 그대로 둠

### AuthResponse.Login에 refreshToken 필드 추가 
지금 `AuthController/AuthService`에서 쓰고 있는 Login 응답 DTO에 refreshToken을 추가
```java
	record Login(
		Long userId,
		String email,
		String nickname,
		String accessToken,
		String refreshToken,
		String message
	) implements AuthResponse {
	}

```

## 4. AuthService.login에서 Refresh Token 발급
```java
// 3-1. 추가 : Refresh Token 생성
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());

		// 3-2 : Refresh Token DB 저장/업데이트
		// .plusSeconds(ONE_WEEK)를 지금 서비스에서 해주고 있지만 엔티티쪽에서 해주면 더 깔끔할 것 같기도 함 고민 해보자
		RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
			.map(rt -> {
				rt.updateToken(refreshTokenValue, LocalDateTime.now().plusSeconds(ONE_WEEK));
				return rt;
			})
			.orElseGet(()-> RefreshToken.create(
				user,
				refreshTokenValue,
				LocalDateTime.now().plusSeconds(ONE_WEEK)
			));

		refreshTokenRepository.save(refreshToken);

```

## 5. /auth/refresh 엔드포인트 추가
### 5-1 요청 DTO
```java
package me.boardApp.auth.dto;

public record RefreshRequest(String refreshToken) {
}

```
### 5-2 AuthService에 refresh() 메서드 추가
```java
public AuthResponse.Login refresh(RefreshRequest request) {
		String refreshTokenValue = request.refreshToken();

		// 1. DB에서 refreshToken 조회
		RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
			.orElseThrow(() -> new AuthorizationException(ExceptionCode.TOKEN_INVALID));

		// 2. 만료 여부 체크
		if (refreshToken.isExpired()) {
			throw new AuthorizationException(ExceptionCode.TOKEN_EXPIRED);
		}

		User user = refreshToken.getUser();

		// 3. 새 Access Token 생성
		String newAccessToken = jwtTokenProvider.generateAccessToken(
			user.getId(),
			user.getEmail(),
			user.getRole().name()
		);

		// 4. 필요하다면 여기서 refreshToken도 재발급. 하지만 지금은 유지로 선택
		return new AuthResponse.Login(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			newAccessToken,
			refreshTokenValue,
			"Access Token 재발급 성공"
		);
	}
```
### 5-3 AuthController에 매핑 추가 
```java
@PostMapping("/refresh")
	public ResponseEntity<AuthResponse.Login> refresh(@RequestBody @Valid RefreshRequest request) {
		AuthResponse.Login response = authService.refresh(request);
		return ResponseEntity.ok(response);
	}
```

## Refresh Token Rotation
Refresh Token을 한 번 쓰면, 반드시 새 Refresh Token으로 교체하는 방식

### Rotation 없는 경우의 문제점 
```text
Refresh Token 탈취됨
↓
공격자가 계속 Access Token 재발급
↓
사용자는 눈치도 못 챔 😱
```
### Refresh Token Rotaion 흐름 (정석)
로그인 시 
1. Access Token 발급
2. Refresh Token 바급
3. Refresh Token을 DB에 저장  
   (user_id, token, 만료시간, revoked=false)

### Access Token 만료 -> /auth/refresh 요청
```java
POST /auth/refresh
Authorization: Bearer <refresh-token>
```
서버 로직
1. Refresh Token 조회
2. DB에 존재하는지 확인
3. 만료 여부 확인
4. revoked 여부 확인  
여기까지 통과함녀 정상 토큰

### 정상일 경우 (Rotation 발생)
```text
기존 Refresh Token → 폐기
새 Refresh Token → 발급 & 저장
새 Access Token → 발급
```
✔ 기존 토큰 : revoked = true
✔ 새 토큰 : revoked = false

## Refresh Token 재사용 감지란?
공격자가 이미 사용된 Refresh Token을 다시 사용  
위험한 이유 : 
- 이미 정상 사용자 쪽에서는 토큰이 교체되었음
- 그럼에도 같은 토큰이 다시 들어왔다? -> 100% 탈취

### 재사용 감지시 대응 전략 
즉시 해야 할 것 : 
- 해당 Refresh Token -> 이미 revoked 상태
- 그런데도 또 들어왔다?
- -> 그 사용자 소유의 모든 Refresh Token 전부 폐기
- 강제 로그아웃 처리

### Refresh Token을 유저당 하나만 유지한다면?
유저당 refresh token 1개 설계는 여러 기기 동시 로그인에 제약이 있다  
이 설계의 전제 : "한 유저는 동시에 하나의 로그인 상태만 유지한다"  

#### 1. 유저당 Refresh Token 1개 = 어떤 의미? 
- A 유저가 노트북에서 로그인
  - Refresh Token A 발급 & DB 저장
- 같은 유저가 모바일에서 로그인
  - Refresh Token B 발급 & DB에서 A를 덮어씀  

결과 : 
- 노트북의 refresh token A -> 무효 
- 모바ㅣㅇㄹ로만 로그인 유지  
즉 , 가장 마지막 로그인만 유효 

#### Refresh Token 유저당 하나만 유지했을 때의 장점
장점 : 
- 보안이 강함
- 토큰 탈취 시 피해 범위 작음
- 구현 단순
- "강제 로그아웃"이 쉬움
- 재사용 감지 로직이 깔끔  

실제로 :
- 사내 서비스 
- 관리자 시스템
- 금융/보안 민감 서비스 
- 이 설계를 일부러 선택하는 경우가 많음

### 여러 기기 지원은 어떻게 할까? (확장 설계)
여러 기기를 지원하려면 토큰의 소유 단위를 바꿔야함  
유저 기준에서 -> 세션(디바이스) 기준으로  
```text
User 1
 ├─ RefreshToken (device A)
 ├─ RefreshToken (device B)
 └─ RefreshToken (device C)
```
보통의 구현  
다중 기기용 Refresh Token 엔티티 예시
```java
@Entity
public class RefreshToken {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = LAZY)
    private User user;

    private String token;

    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String deviceId; // ⭐ 핵심 (UUID, User-Agent hash 등)
}
```
그리고 조회 기준이 바뀜 👇
```java
findByUserAndDeviceId(user, deviceId)
```
### 로테이션은 어떻게 ? 
기기별 로테이션
- 모바일 -> 모바일 토큰만 갱신
- 노트북 -> 노트북 토큰 유지  

재사용 감지
- 해당 deviceId 범위에서만 revoke
- 다른 기기는 영향 없음

이 프로젝트는 Spring Security + JWT 기반의 인증/인가 구조를 사용했습니다  
보안 안정성과 설계 명확성을 우선으로 하여 단일 Refresh Token 전략 + Rotation + 재사용 감지를 적용했습니다.

## 로그아웃의 책임을 user가 아닌 auth 로 이동
### 기존 
customUserDetails 기반 로그아웃  
장점 :
- 구현이 간단
- 프론트에서 refresh token을 안보내도 됨
- "현재 로그인한 사용자"개념에 직관적  
한계 : 
- "어떤 세션(토큰)을 로그아웃할지"를 알 수 없음
- 단일/다중 기기 구분 불가
- 토큰 탈취 상황에서 정확한 토큰 무효화가 불가능  

즉, 유저는 알지만, 세션(토큰)은 모른다  

### 변경
Refresh Token 기반 로그아웃 
```java
public void logout(String refreshTokenValue)
```
장점 : 
- 정확히 이 토큰을 무효화
- 단일/다중 기기 모두 확장 가능
- 재사용 감지 / 보안 이벤트 기록 가능
- JWT 철학과 맞음 (stateless + server-side control)  
단점 :
- 프론트에서 refresh token을 보내야함
- 코드가 조금 더 복잡 
- -> 이런 복잡함은 보안 비용이라 생각함
