# Spring Security 핵심 구조 - 인증, 인가, 필터 구조 

## Spring Security의 목적
1. **인증 (Authentication)**
   - 이 사용자가 누구인가?
2. **인가 (Authorization)**
   - 이 사용자가 이 자원에 접근할 권한이 있는가?

Spring Security는 이 두 문제를  
**필터 기반 구조**로 일관되게 처리한다. 

---

## 필터 기반 구조
Spring Security는 Servlet Filter 기준으로 동작한다  
클라이언트 요청은 DispatcherServlet에 도달하기 전에  
여러 보안 필터를 거쳐 간다.  

이 필터들은 `SecurityFilterChain`에 순서대로 등록되어 실행된다.

| 필터                                   | 역할                 |
|--------------------------------------|--------------------|
| UsernamePasswordAuthenticationFilter | 로그인 요청 시 사용자 인증 처리 |
| ExceptionTranslationFilter           | 인증/인가 예외를 처리       |
| FilterSecurityInterceptor            | 인가(접근 권한 검사) 처리    |

> 즉, **컨트롤러에 도달하기 전에 보안 검사가 끝난다**


## 인증(Authentication) 과정
사용자가 로그인을 시도하면 흐름은 다음과 같다  

1. 클라이언트가 `/login`으로 아이디,비밀번호 전송
2. `UsernamePasswordAuthenticationFilter`가 요청을 가로채서 `Authentication` 객체 생성
3. `AuthenticationManager`에게 인증 요청 전달
4. `AuthenticationManager`는 등록된 `AuthenticationProvider`에게 인증 위임
5. 'AuthenticationProvider' 가 실제 인증 수행
6. `UserDetailsService`를 통해 사용자 정보 조회 
7. `PasswordEncoder`로 비밀번호 비교 
8. 일치하면 인증 성공, `SecurityContextHolder`에 인증 정보 저장
9. 이후 요청마다 `SecurityContextHolder` 에서 인증 정보를 꺼내 사용

## 인가(Authorization) 과정
인증이 끝났다고 해서 모든 요청이 허용되지는 않는다    
이제 "이 사용자가 이 요청을 할 수 있는가?"를 검사해야 한다  

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin")
public String adminPage() { ... }
```
- Spring Security는 현재 인증된 사용자의 권한을 확인
- 조건을 만족하지 않으면 요청을 차단
- 컨트롤러 로직은 실행되지 않는다 
- 이 인가 과정은 필터 기반 인가 또는 메서드 보안 (`@PreAuthorize`)으로 처리된다 

## SecurityContext와 인증 정보 
인증이 성공하면 `Authentication` 객체가  
`SecurityContextHolder`에 저장된다  
- `SecurityContextHolder`는 ThreadLocal 기반
- 요청마다 인증 정보가 안전하게 유지된다  

컨트롤러나 서비스에서 다음과 같이 접근 가능하다
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

## 📚 마무리 정리
- Spring Security는 **필터 기반 구조**로 인증/인가를 처리한다
- 인증 -> 인가 순서로 동작한다
- 인증 결과는 SecurityContextHolder에 저장된다
- 컨트롤러 로직은 보안 검사를 통과한 이후에만 실행된다 

### 다음 글에서는 
Spring Security가 요구하는  
**UserDetails/ UserDetailsService 는 왜 필요한지**,  
이미 존재하는 UserService와 어떤 역할 차이가 있는지 정리해본다 