# JWT 전체 흐름 + 코드 

## JWT 인증 구조 전체 흐름 요약 
```java
클라이언트 요청
 → JwtAuthenticationFilter
 → (인증 성공 시) SecurityContextHolder 세팅
 → Controller (@AuthenticationPrincipal 사용 가능)
 → 권한 부족 시 AccessDeniedHandler (403)
 → 인증 실패 시 AuthenticationEntryPoint (401)
```

## 1️⃣ AuthController - 인증 API의 진입점 
```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
```
역할 한 줄 요약 
- JWT 기반 인증/인가와 관련된 "외부 API 진입점"

### /auth/login - JWT 로그인 
```java
@PostMapping("/login")
public ResponseEntity<AuthResponse.Login> login(
    @RequestBody @Valid UserRequest.Login request
)
```
여기서 하는 일 
1. 이메일 / 비밀번호 검증
2. Access Token + Refresh Token 발급 
3. 응답 DTO로 토큰 반환  
  
👉**세션 로그인과 완전히 다른 흐름**
👉서버는 로그인 상태를 기억하지 않음 

### /auth/logout
```java
@PostMapping("/logout")
public ResponseEntity<?> logout(@RequestBody RefreshRequest request)
```
- Refresh Token을 DB에서 REVOKED 처리 
- Access Token은 만료될 때까지 자연스럽게 사라짐   
👉JWT의 로그아웃은 **"토큰 무효화 정책"의 문제**라는걸 잘 보여주는 코드 

### /auth/refresh
```java
	@PostMapping("/refresh")
public ResponseEntity<AuthResponse.Login> refresh(@RequestBody @Valid RefreshRequest request) {
	AuthResponse.Login response = authService.refresh(request);
	return ResponseEntity.ok(response);
}
```
- Access Token 만료 시 
- Refresh Token으로 새 Access Token 발급  
👉/auth/refresh는 `permitAll()`이지만  
👉**실질적 인증은 refreshToken 검증으로 수행**

### /auth/me
```java
@GetMapping("/me")
	public ResponseEntity<AuthResponse.Me> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
		// JwtAuthenticationFilter 덕분에 여기 올 때 이미 인증된 상태
		AuthResponse.Me response = new AuthResponse.Me(
			userDetails.getId(),
			userDetails.getEmail(),
			userDetails.getNickname(),
			"현재 로그인된 사용자 정보"
		);

		return ResponseEntity.ok(response);
	}
```
💡핵심 포인트
- 이 메서드는 **JWT 필터를 통과한 이후에만 호출됨**
- CustomUserDetails는 : 
  - JwtAuthenticationFilter가 
  - SecurityContextHoler에 넣어준 결과물 
  
👉이 한API로  
**"JWT 인증이 실제로 동작한다"는걸 증명 

## 2️⃣AuthResponse / RefreshRequest - 인증 전용 DTO
### AuthResponse (sealed interface)
```java
package me.boardApp.auth.dto;

public sealed interface AuthResponse permits AuthResponse.Login, AuthResponse.Me {
	record Login(
		Long userId,
		String email,
		String nickname,
		String accessToken,
		String refreshToken,
		String message
	) implements AuthResponse {
	}

	record Me(
		Long userId,
		String email,
		String nickname,
		String message
	) implements AuthResponse {
	}
}
```
- 인증 응답을 하나의 타입으로 묶음 
- 로그인 / 내 정보 응답이 의미적으로 연결  
👉"Auth 도메인"이 명확

### RefreshRequest
```java
public record RefreshRequest(@NotBlank String refreshToken) {}
```
- Refresh Token 만 전달받는 요청 DTO
- Access Token 없이 호출되는 API 특성에 맞음 

## 3️⃣JwtAuthenticationFilter - JWT 인증의 핵심 
```java
public class JwtAuthenticationFilter extends OncePerRequestFilter
```
역할 : 매 요청마다 JWT를 검사하고, 인증 정보를 SecurityContext에 저장하는 필터  

❗️이 필터가 하는 일
1. Authorization 헤더 읽기
2. Bearer <token> 형식 확인 
3. 토큰 검증 (서명 / 만료)
4. 사용자 조회
5. Authentication 생성
6. SecurityContextHolder에 저장
---
왜 OncePerRequestFilter? 
- 요청당 딱 한번만 실행
- 중복 인증 방지
---
```java
SecurityContextHolder.getContext().setAuthentication(auth);
```
👉 이 한 줄 덕분에 : 
- `@AuthenticationPrincipal`
- `@PreAuthorize`
- `hasRole()`  
    전부 가능해짐 

## 4️⃣JwtTokenProvider - JWT 유틸리티의 중심 
```java
@Component
public class JwtTokenProvider
```
역할 : JWT 생성 / 검증 / 파싱을 전담하는 컴포넌트  

### 핵심 메서드들
#### 토큰 생성
```java
generateAccessToken(...)
generateRefreshToken(...)
```
- Access Token
  - userId, email, role 포함
- Refresh Token
  - subject(userId)만 포함 -> 보안적으로 안전 

#### 토큰 검증 
```java
validateToken(String token)
```
- 서명 위조
- 만료
- 형식 오류  
    전부 AuthorizationException으로 변환 
  
👉컨트롤러까지 안가고  
👉**Security 계층에서 차단**

#### Claims 파싱
```java
getUserId()
getEmail()
getRole()
```
- JWT는 **stateless**
- 필요한 정보는 토큰 안에 다 들어있다 

## 5️⃣JwtAuthenticationEntryPoint - 인증 실패 (401) 
```java
@Component
public class JwtAuthenticationEntryPoint
```
언제 호출될까? 
- 인증 정보가 없거나
- 토큰이 잘못됐을 때  
👉**401 Unauthorized**

### 이 설계의 좋은 점 
- Filter에서 예외를 직접 처리하지 않음
- EntryPoint에서 응답 형식 통일  
👉전역 예외 처리와도 잘 어울림 

## 6️⃣JwtAccessDeniedHandler - 권한 부족 (403)
```java
@Component
public class JwtAccessDeniedHandler
```
언제 호출? 
- 인증은 됐지만
- ROLE_ADMIN 같은 권한 부족  
👉 **403 Forbidden**

---
### 인증 vs 인가 분리 
| 상황           | 응답  |
|--------------|-----|
| 토큰 없음 / 만료   | 401 |
| 토큰 유효, 권한 없음 | 403 |

## 7️⃣RefreshToken 엔티티 - 보안의 핵심 
```java
@Entity
public class RefreshToken
```
왜 DB에 저장?
- 로그아웃 
- 재사용 감지
- 강제 만료
- 계정 탈취 대응  
👉 Refresh Token은 **세션의 역할**
---
### Status enum
```java
ACTIVE / REVOKED
```
- 단순 삭제 ❌
- 이력 기반 보안 설계  
👉refresh reuse 감지 가능 

## 8️⃣AuthService - 인증 비즈니스 로직 
역할 : JWT 발급 / 재발급 / 폐기를 담당하는 인증 도메인 서비스 
---
### login()
- 사용자 검증
- 기존 refresh 전부 revoke
- 새 access / refresh 발급  
👉 **동시 로그인 정책을 코드로 표현**

---
### refresh()
- refresh Token 검증
- 재사용 감지
- 계정 잠금까지 고려했음 
---
### logout()
- refresh token revoke
- idempotent(멱등성) 처리

## 9️⃣AuthorizationService - 도메인 권한 검증 
```java
@Service
public class AuthorizationService
```
이 클래스의 의미 
- Spring Security가 하는 건: 
  - "접근 가능?"
- 이 클래스가 하는 건: 
  - "도메인 규칙상 가능한가?"  
👉인증/인가 vs 도메인 권한 분리 