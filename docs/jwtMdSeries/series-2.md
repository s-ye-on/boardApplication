# Spring security + JWT 인증 구현하기 (Token · Filter · Config) (JWT 2편)
- JwtTokenProvider · JwtAuthenticationFilter · SecurityConfig

## 이 글의 목표 
1 편에서 JWT와 Spring Security의 전체 구조를 이해했다면,  
이번 글에서는 **실제로 어떻게 구현되는지**를 정리한다  

이 글을 읽고 나면 :  
- JwtTokenProvider는 왜 필요한지 
- JwtAuthenticationFilter가 정확히 무슨 일을 하는지
- 왜 OncePerRequestFilter를 쓰는지
- SecurityConfig에서 어떤 설정이 바뀌는지  

를 코드 흐름 기준으로 복기할 수 있다 

>📌 이 글은 “이론 예제”가 아니라
>   내 게시판 프로젝트에서 실제로 사용한 구조 기준으로 설명한다.

---

## JWT 구현에서 추가되는 핵심 컴포넌트 3가지 
JWT 기반 인증을 쓰기 위해  
Spring Security 기본 구조에 딱 3가지만 추가하면 된다.  

1. **JwtTokenProvider**
   - 토큰 생성 / 파싱 / 검증 담당 

2. **JwtAuthenticationFilter**
   - 매 요청마다 토큰을 검사하는 필터 

3. **SecurityConfig 수정**
   - 세션 비활성화 + JWT 필터 등록 

---

## 1️⃣JwtTokenProvider - 토큰 생성과 검증의 책임자 

**왜 필요한가?**  
JWT 관련 로직을  
- 컨트롤러
- 서비스 
- 필터  
여기 저기 흩어두면 **유지보수 지옥**이 된다  
👉 그래서 JWT 관련 책임을 **한 클래스에 모았다**

---

### JwtTokenProvider의 역할 요약
- Access Token / Refresh Token 생성
- JWT 서명 검증
- 만료 여부 확인 
- 토큰에서 userId / role 추출
- (Refresh Token은 별도 DB(Entity)로 관리하며,  
    Provider는 생성/만료 계산 역할만 담당)

--- 
### 핵심 코드 구조 
```java
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secretKeyPlain;

    @Value("${app.jwt.expiration-seconds}")
    private long tokenValidityInSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKeyPlain.getBytes());
    }
}
```
왜 @PostConstruct를 썼나? 
- 서버 시작 시 한 번만 실행
- JWT 서명에 사용할 Key 객체를 미리 만들어 둠 
- 매 요청마다 key 생성 ❌

---

### Access Token 생성 
```java
public String generateAccessToken(Long userId, String email, String role) {
    long now = System.currentTimeMillis();
    Date validity = new Date(now + tokenValidityInSeconds * 1000);

    return Jwts.builder()
        .setSubject(String.valueOf(userId))  // sub : userId
        .claim("email", email)
        .claim("role", role)
        .setIssuedAt(new Date(now))
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
}
```
✔ JWT에는 **최소한의 정보만** 담는다
- sub : userId (식별자)
- role : 권한 판단용 
- email : 클라이언트 식별/편의용  

---

### 토큰 검증 
```java
	public void validateToken(String token) {
	try {
		Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token);
	} catch (SecurityException | MalformedJwtException e) {
		// 잘못된 JWT 서명
		throw new AuthorizationException(ExceptionCode.TOKEN_INVALID);
	} catch (ExpiredJwtException e) {
		//토큰 만료
		throw new AuthorizationException(ExceptionCode.TOKEN_EXPIRED);
	} catch (UnsupportedJwtException e) {
		throw new AuthorizationException(ExceptionCode.TOKEN_UNSUPPORTED);
	} catch (IllegalArgumentException e) {
		throw new AuthorizationException(ExceptionCode.TOKEN_INVALID);
	}
}
```
👉**검증 실패 시 boolean 반환 ❌**  
👉**예외를 던진다 ⭕️**  

이렇게 해야:  
- 필터
- EntryPoint
- 전역 예외 처리기  
에서 **일관된 에러 흐름**을 만들 수 있다 

---

## 2️⃣JwtAuthenticationFilter - 매 요청마다 인증 처리 

### 왜 필터가 필요한가?  
JWT는 세션처럼 "로그인 상태"가 저장되지 않는다  

👉**요청이 올 때마다**
- 토큰을 읽고
- 검증하고
- 인증 정보를 다시 만들어야한다  

이 역할은 **Filter**가 가장 적절하다

---

### 왜 OncePerRequestFilter인가? 
- 한 요청당 **딱 한 번만 실행 보장**
- 중복 인증 방지
- Spring Security에서 JWT 필터 구현 시 사실상 표준

---

### 필터의 전체 흐름 
```text
요청
 → Authorization 헤더 확인
 → 토큰 검증
 → 사용자 조회
 → Authentication 생성
 → SecurityContextHolder에 저장
 → 다음 필터
```
---

### 핵심 코드 
```java
	@Override
	protected void doFilterInternal(HttpServletRequest request,
																	HttpServletResponse response,
																	FilterChain filterChain)
		throws ServletException, IOException {
		// HTTP Authorization 헤더 읽기 (예상 : "Bearer <토큰>")
		String authHeader = request.getHeader("Authorization");

		// Authorization 헤더가 없으면 이 필터는 건너 뛰고, 나머지 체인에 맡긴다
		// 토큰 없는 요청은 익명 요청으로 두고, 다음 필터로 넘김 나중에 시큐리티가 401/403 판단
		// 이렇게 해야 /login 같은 공용 API 호출 가능
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String token = authHeader.substring(7);

			// 유효성 검사 (문제 있으면 AuthorizationException 발생)
			// (서명/만료 등)
			jwtTokenProvider.validateToken(token);

			Long userId = jwtTokenProvider.getUserId(token);
			// 지금은 user가 없으면 null로 반환하고 그냥 통과 시키고 있음
			// 토큰은 유효하지만 해당 유저가 없는 경우 -> 삭제, 탈퇴 일 수 있음
			// null 반환이 아니라 예외를 던져서 처리하는걸로 수정해서 막자
			// 삭제된 유저의 토큰까지 막기
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new AuthorizationException(ExceptionCode.TOKEN_INVALID));

			if (user != null) {
				// User 엔티티 -> Spring Security UserDetails로 변환
				CustomUserDetails principal = new CustomUserDetails(user);

				// Authentication 객체 생성
				Authentication auth = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					principal.getAuthorities()
				);
				// SecurityContext에 인증 정보 저장 -> 이후 Controller에서 @AuthenticationPrincipal 등으로 사용 가능
				SecurityContextHolder.getContext().setAuthentication(auth);
			}

			filterChain.doFilter(request, response);

		} catch (AuthorizationException ex) {
			// 여기서 직접 응답을 써도 되고, request attribute에 태워서 EntryPoint로 넘겨도 됨
			// 보통은 EntryPoint에서 공통 처리하는게 깔끔하니 attribute에 태우자
			request.setAttribute("authError", ex);
			// 나머지는 SecurityException 흐름을 넘기기 위해 filterChain을 계속 타게 두지 말고 종료
			// 대신 EntryPoint가 호출되도록 doFilter를 호출하지 않고 바로 처리하고 return 하는 방법도 있음
			// 여기서 sendError 하지말고, 시큐리티가 EntryPoint를 호출하게 맡기는 쪽이 좋음
			throw ex; // ApiException / AuthorizationException 전역 핸들러 또는 EntryPoint로 전달
		}
	}
```
---

### 여기서 가장 중요한 포인트 ⭐️
```java
SecurityContextHolder.getContext().setAuthentication(auth);
```
이 한 줄 덕분에 : 
- `@AuthenticationPrincipal`
- `hasRole()`
- `@PreAuthorize`  
가 **세션 기반일 때와 동일하게 동작**한다  
👉 JWT를 써도 컨트롤러/서비스 코드를 거의 안바꾸는 이유이다 

---

## 3️⃣SecurityConfig - JWT 기준으로 보안 정책 전환 
JWT 기반 인증에서 SecurityConfig의 핵심은 딱 3가지다  

---

### ① 세션 비활성화
```java
.sessionManagement(sm ->
    sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```
👉Spring Security에게 말하는 것 :  
"이 앱은 세션안써. 상태 저장하지마"

---

### ② 폼 로그인 / 로그아웃 비활성화
```java
.formLogin(AbstractHttpConfigurer::disable)
.logout(AbstractHttpConfigurer::disable)
```
- JWT는 API 기반 인증 
- /login 폼, 세션 로그아웃 필요 없음 

---

### ③ JWT 필터 등록 
```java
.addFilterBefore(
    jwtAuthenticationFilter,
    UsernamePasswordAuthenticationFilter.class
)
```
👉 UsernamePasswordAuthenticationFilter는  
    formLogin 기반 로그인 필터이기 때문에  
    JWT 인증에서는 사실상 사용되지 않는다.  

JWT 필터를 그 앞에 두는 이유는  
SecurityContext를 미리 채워서  
뒤의 인가 판단(@PreAuthorize 등)이 정상 동작하게 하기 위함이다  

즉, JWT 인증은  
"로그인을 처리하는 것"이 아니라  
"인가 판단을 가능하게 만드는 전처리 단계"에 가깝다  

---

### 최종 SecurityConfig 요약 
```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .formLogin(AbstractHttpConfigurer::disable)
    .logout(AbstractHttpConfigurer::disable)
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/auth/login", "/auth/refresh").permitAll()
        .anyRequest().authenticated()
    )
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

--- 

## 📚마무리 정리 
- JWT 구현의 핵심은 **TokenProvider + Filter**
- JWT 인증에서도 Spring Security 구조는 그대로 유지된다 
- 인증 결과는 항상 SecurityContextHolder에 저장된다 
- 필터에서 Authentication을 세팅하면  
  - 컨트롤러
  - 서비스
  - 메서드 보안  
  을 그대로 사용할 수 있다 

### 다음 글 예고 
다음 글에서는  
JWT 인증 실패를 어떻게 처리했는지를 다루겠다  
- AuthenticationEntryPoint는 언제 호출되는가 
- AccessDeniedHandler와의 차이
- 401 / 403 응답을 JSON으로 통일한 이유
- 필터에서 던진 예외를 어떻게 전달했는가  

👉[3편] JWT 인증 실패 처리 설계 - 401 vs 403