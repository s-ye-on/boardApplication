# JWT 코드 설명

## JwtAuthenticationEntryPoint - 인증 실패 응답 담당
```java
package me.boardApp.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.boardApp.global.exception.AuthorizationException;
import me.boardApp.global.exception.ExceptionCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증 실패 응답 처리
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
```java
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
```
- Spring Security가 인증 실패할 때 자동으로 호출되는 컴포넌트

```java
Object authError = request.getAttribute("authError");
```
- 필터에서 던진 AuthorizationException이 있는지 확인

```java
ExceptionCode exceptionCode = (authError instanceof AuthorizationException authorizationException)
    ? authorizationException.getExceptionCode()
    : ExceptionCode.UNAUTHORIZED;
```
- 만약 필터에서 넣어둔 에러 정보가 있다면, 그걸 사용하고 없다면 그냥 "인증 필요(401)"로 설정

```java
response.setStatus(exceptionCode.getStatus().value());
response.setContentType("application/json;charset=UTF-8");
```
- HTTP Response에 상태코드 세팅하고 JSON 타입 지정

```java
response.getWriter().write(body);
```
- 클라이언트에게 JSON 형태로 응답


## JwtAuthenticationFilter - 요청마다 토큰 확인
```java
package me.boardApp.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.exception.AuthorizationException;
import me.boardApp.global.exception.ExceptionCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
		if(authHeader == null || !authHeader.startsWith("Bearer ")) {
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
				.orElseThrow(()-> new AuthorizationException(ExceptionCode.TOKEN_INVALID));

			if(user != null){
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
```java
public class JwtAuthenticationFilter extends OncePerRequestFilter
```
- 모든 요청마다 실행되지만 한 요청당 한 번만 실행되는 보안 필터.

```java
String authHeader = request.getHeader("Authorization");
```
- 헤더에서 토큰 꺼냄

```java
if(authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}
```
- 토큰이 없으면 익명 사용자로 처리 -> 다음 필터로 넘김 
- (이렇게 해야 /login 같은 공용 API 호출 가능)

```java
jwtTokenProvider.validateToken(token);
```
- 서명, 만료 여부, 형식을 체크하고 문제 있으면 예외 발생

```java
Long userId = jwtTokenProvider.getUserId(token);
User user = userRepository.findById(userId).orElse(null);
```
- 토큰에서 추출한 userId로 DB에서 사용자 조회

```java
Authentication auth = new UsernamePasswordAuthenticationToken(
    principal, null, principal.getAuthorities()
);
SecurityContextHolder.getContext().setAuthentication(auth);
```
- 핵심 부분
- JWT 기반 로그인에서는 인증 저장 방식이 다름 
- -> 로그인할 때 세션을 만들지 않고, 요청마다 토큰을 검증해서 인증 상태를 넣음
