# JWT 인증 실패 처리 설계하기 (401 vs 403)
- AuthenticationEntryPoint · AccessDeniedHandler · Filter 예외 흐름  
  (JWT 3편)

## 이 글의 목표
JWT 인증을 구현하다 보면 이런 질문이 반드시 생긴다 
- 토큰이 없으면 왜 401일까? 
- 토큰은 있는데 권한이 없으면 왜 403일까? 
- 필터에서 예외를 던졌는데 컨트롤러까지 안오고 응답이 나가는 이유?
- AuthenticationEntryPoint와 AccessDeniedHandler는 **언제, 왜** 호출될까?  

이 글에서는  
👉**JWT 인증 실패 흐름을 Spring Security 내부 동작 기준으로 정리**한다  

>📌 이 글은
“예외를 어디서 처리하느냐”가 아니라
“누가 언제 호출되느냐” 에 초점을 둔다.

--- 

## 시작 전 한 줄 요약
| 상황             | 상태코드 | 처리 주체                    |
|----------------|------|--------------------------|
| 인증 자체가 안 됨     | 401  | AuthenticationEntryPoint |
| 인증은 됐지만 권한 부족  | 403  | AccessDeniedHandler      |

---

## 1️⃣401 vs 403의 본질적 차이 

### 401 Unauthorized
- 인증(Authentication) 실패
- 사용자가 누구인지 확인되지 않음  

예시 :  
- Authorization 헤더 없음 
- JWT 만료
- JWT 서명 오류
- 토큰 파싱 실패  

👉"너 누구야?" 단계에서 실패

---

### 403 Forbidden
- 인가(Authorization) 실패
- 사용자는 확인됐지만 권한이 없음  

예시 :  
- USER가 ADMIN API 호출
- `@PreAuthorize` 조건 불일치  

👉"너인 건 알겠는데, 이건 안돼"

---

## 2️⃣Spring Security에서 실패 흐름은 어디서 갈리는가 

### 전체 흐름 개요
```text
요청
 → Security Filter Chain
 → JwtAuthenticationFilter
 → SecurityContext 세팅 여부
 → 인가 판단
 → 실패 시 EntryPoint or AccessDeniedHandler
```
여기서 **분기점은 딱 하나**다  
👉SecurityContextHoler에  
👉**Authentication이 있느냐 / 없느냐**

---

## 3️⃣JwtAuthenticationFilter에서의 역할 (인증 전 단계)

### JwtAuthenticationFilter code
```java
// 매 요청마다 토큰 검사
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	// 역할 :
	// 매 요청 마다 Authorization: Bearer <token> 헤더를 읽고
	// 토큰 유효성 검사 -> OK면 SecurityContext에 인증 정보 세팅

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

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
}
```

### 필터의 책임 
- Authorization 헤더 읽기
- JWT 검증
- 사용자 조회
- Authentication 생성
- SecurityContext에 저장

```java
SecurityContextHolder.getContext().setAuthentication(auth);
```
이 줄이 실행되면 : 
- 인증 성공
- 이후 단계는 **인가 판단**  

이 줄이 실행되지 않으면 :  
- 인증 실패
- 이후 단계는 **401 처리**

---

## 4️⃣AuthenticationEntryPoint - 401의 시작점 

### 언제 호출될까? 
- 인증되지 않은 사용자가 
- 인증이 필요한 리소스에 접근할 때   

👉**SecurityContext에 Authentication이 없는 상태**

---

### 내가 만든 EntryPoint 코드
```java
// 인증 실패 응답 처리 401
@Component // Spring Security가 인증 실패할 때 자동으로 호출되는 컴포넌트
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		Object authError = request.getAttribute("authError");
		ExceptionCode exceptionCode = (authError instanceof AuthorizationException authorizationException)
			? authorizationException.getExceptionCode()
			: ExceptionCode.UNAUTHORIZED;

		response.setStatus(exceptionCode.getStatus().value());
		response.setContentType("application/json;charset=UTF-8");

		String body = """
			{
			  "code": "%s",
			  "message": "%s"
			}
			""".formatted(exceptionCode.name(), exceptionCode.getMessage());

		response.getWriter().write(body);
	}
}
```

---
### 핵심 포인트 ⭐️
```java
request.setAttribute("authError", ex);
```
JwtAuthenticationFilter에서 던진 예외를  
**request attribute로 전달**하고,  
EntryPoint에서 그걸 꺼내서 응답을 결정  

👉필터는 **판단만**  
👉EntryPoint는 **응답 책임**  

~~이 역할 분리가 설계적으로 깔끔하지 않나요?~~

---

## AccessDeniedHandler - 403의 시작점 

### 언제 호출될까? 
- Authentication은 존재
- 하지만 권한(Role)이 부족  

예 :  
```java
@PreAuthorize("hasRole('ADMIN')")
```
USER로 접근 -> ❌  

--- 

내가 만든 AccessDeniedHandler
```java
// 인증은 있지만 Role 부족 -> 여기서 403 응답
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
		
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("""
			{"code":"FORBIDDEN","message":"이 리소스에 접근할 권한이 없습니다."}
			""");
	}
}
```
👉이 단계에서는  
- 토큰 검증 ❌
- 사용자 조회 ❌  
이미 다 끝난 상태

---

## 6️⃣ SecurityConfig에서 두 핸들러를 연결하는 이유 
```java
.exceptionHandling(ex -> ex
	.authenticationEntryPoint(authenticationEntryPoint)
	.accessDeniedHandler(accessDeniedHandler)
)
```
이 설정 덕분에 : 
- 인증 실패 -> EntryPoint
- 인가 실패 -> AccessDeniedHandler  

👉**컨트롤러까지 가지 않는다**
👉**Spring Security 필터 단계에서 요청 처리가 종려되어,  
DispatcherServlet 및 Controller는 전혀 호출되지 않는다**

--- 

## 7️⃣왜 전역 예외 처리기 (@RestControllerAdvice)가 아닌가? 
중요한 포인트이다 
- 필터는 **컨트롤러 밖**
- DispatcherServlet 이전 단계 
- 그래서 `@ExceptionHandler`가 동작하지 않는다

👉**보안 예외는 보안 레이어에서 끝내야한다**  

좀 더 풀어서 얘기하자면,  
- 401 / 403은 "비즈니스 예외"가 아니라  
- "보안 진입 차단"이기 때문에  
    전역 예외 처리기의 관할이 아니다 

이게 Spring Security 철학

---

## 📚마무리 정리 
- 401과 403의 차이는 **인증 vs 인가**
- Authentication이 없으면 -> 401 -> EntryPoint
- Authentication은 있지만 권한(Role)이 없으면 -> 403 -> AccessDeniedHandler
- JWT 필터는 **응답을 만들지 않는다**
- 보안 예외는 Security 레이어에서느 처리하는 것이 정석
- 이 구조 덕분에 컨트롤러 코드는 꺠끗하게 유지할 수 있다
- **보안 예외는 비즈니스 예외가 아니다.**
  - 인증 / 인가 실패는 컨트롤러에 진입하기 전에 Security 레이어에서 처리되어야 하며,  
    이 책임은 `@RestController` 가 아니라  
    `AuthenticationEntryPoint` 와 `AccessDeniedHandler` 가 가진다  

---

### 다음 글 예고 (마지막 편)
다음 글에서는  
**RefreshToken 설계와 보안 정책**을 정리하겠다
- 왜 Refresh Token이 필요한가
- DB에 저장하는 이유
- Rotation / Revoke 차이
- Refresh Token 재사용 감지 
- 정책 1/ 2/ 3 비교  

👉[4편] Refresh Token 설계와 보안 정책 비교 