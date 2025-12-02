## Spring Security

### Spring Security의 목적
1. 인증 (Authentication) : 이 사용자가 누구인가?
2. 인가 (Authorization) : 이 사용자가 이 자원에 접근할 권한이 있는가?

### 필터 기반 구조
- Spring Security는 Servlet Filter 기준으로 동작
- 클라이언트가 요청을 보낼 때, DispatcherServlet에 도달하기 전에 여러 보안 필터를 거쳐감
- 이 필터들은 SecurityFilterChain에 순서대로 등록돼서 실행됨

| 필터                                   | 역할                 |
|--------------------------------------|--------------------|
| UsernamePasswordAuthenticationFilter | 로그인 요청 시 사용자 인증 처리 |
| ExceptionTranslationFilter           | 인증/인가 예외를 처리       |
| FilterSecurityInterceptor            | 인가(접근 권한 검사) 처리    |


### 인증(Authentication) 과정
사용자가 로그인을 시도하면 흐름은 이렇게 흘러감
1. 클라이언트가 / login으로 아이디,비밀번호 전송
2. `UsernamePasswordAuthenticationFilter`가 요청을 가로채서 `Authentication` 객체를 생성
3. 이걸 `AuthenticationManager`에게 전달
4. `AuthenticationManager`는 등록된 `AuthenticationProvider`에게 인증 위임
5. `UserDetatilsService`가 DB에서 사용자 정보(UserDetails) 조회
6. 비밀번호를 비교 (PasswordEncoder 이용)
7. 일치하면 인증 성공, SecurityContextHolder에 인증 정보 저장
8. 이후 요청마다 SecurityContextHolder에서 인증 정보를 꺼내 사용

### 인가(Authorization) 과정
인증이 끝났다해서 다 된게 아님 이제 "누가 접근할 수 있냐"를 판단해야 함

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin")
public String adminPage() { ... }
```
- 이런식으로 접근 제한 걸면 Spring Security는 인증된 사용자의 권한을 확인해서 접근을 허용하거나 거부함

### SecurityContext
인증이 끝나면 SecurityContextHoler에 Authentication 객체가 저장
이건 ThreadLocal 기반이라 요청마다 인증 정보를 따로 유지할 수 있음

컨트롤러에서도 쉽게 접근 가능함 
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

### 구성 요소 요약
| 구성 요소                  | 역할                                           |
|------------------------|----------------------------------------------|
| UserDetails            | 사용자 정보(아이디, 비밀번호, 권한 등)                      |
| UserDetailsService     | 사용자 정보를 DB에서 꺼내오는 역할                         |
| Authentication         | 인증 정보(Principal, Credentials, Authorities 등) |
| AuthenticationProvider | 인증 로직을 직접 구현하는 곳                             |
| SecurityContextHolder  | 현재 인증 정보를 담고 있는 곳                            |
| PasswordEncoder        | 비밀번호 암호화/ 비교 담당                              |