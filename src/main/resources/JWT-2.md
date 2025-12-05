# JWT 추가 확장

## 토큰 만료 / 인증 실패 응답 정리
지금은 유효하지 않은/만료된 토큰이면 JwtAuthenticationFilter안의 validateToken에서 false를 리턴하고, </br>
아무 인증도 세팅되지 않은 채로 anyRequest().authenticated()에 걸리면 기본 401/403 + 빈 바디가 나갈 가능성이 큼

### AuthenticationEntryPoint 추가 (401 응답 커스터마이징)
인증 안된 요청(토큰 없음/잘못됨)에 공통 JSON을 주자 : 
```java
package me.boardApp.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {"code":"UNAUTHORIZED","message":"인증이 필요합니다."}
            """);
    }
}
```
### AccessDeniedHandler 추가 (403 응답 커스터마이징) - 이건 선택임
ROLE은 있지만 권한이 부족할 때 (예: USER가 ADMIN API를 호출) :
```java
package me.boardApp.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
### SecurityConfig에 등록
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                               JwtAuthenticationFilter jwtAuthenticationFilter,
                                               JwtAuthenticationEntryPoint authenticationEntryPoint,
                                               JwtAccessDeniedHandler accessDeniedHandler) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/",
                "/login",
                "/users/join",
                "/users/join-form",
                "/auth/login",
                "/boards.html",
                "/h2-console/**",
                "/css/**",
                "/js/**",
                "/images/**"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```
이렇게 하면 : 
- 토큰 없음/잘못됨 -> 401 + {"code":"UNAUTHORIZED", ...}
- ROLE 안 맞음 -> 403 + {"code":"FORBIDDEN", ...}
브라우저/JS에서 에러 처리가 더 쉬워짐

## 현재 로그인 유저 정보 조회 API(/auth/me)
지금은 프론트에서 "현재 누가 로그인 했는지"를 알기 위해 응답에서 토큰만 보고 있는데, </br>
"나 자신" 정보를 주는 /auth/me 를 하나 만들어 두면 :
- 프론트가 새로 고침해도 /auth/me 한 번 호출해서 유저 정보를 다시 가져올 수 있고
- 나중에 프로필, 마이페이지 등 만들 때도 그대로 재사용 가능

AuthController에 추가 : 
```java
@GetMapping("/me")
public ResponseEntity<LoginResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
    // JwtAuthenticationFilter 덕분에 여기 올 때 이미 인증된 상태
    LoginResponse response = new LoginResponse(
            userDetails.getId(),
            userDetails.getEmail(),
            userDetails.getNickname(),
            null, // 토큰은 여기서 다시 줄 필요 X
            "현재 로그인된 사용자 정보"
    );
    return ResponseEntity.ok(response);
}
```
그리고 /auth/me 는 SecurityConfig에서 authenticated() 대상이므로 :
- 헤더에 Authorization: Bearer <token>이 있어야 200
- 없으면 401 (AuthenticationEntryPoint가 처리)