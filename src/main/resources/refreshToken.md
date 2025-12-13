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