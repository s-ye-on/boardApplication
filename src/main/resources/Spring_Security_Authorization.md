# warp 굳

# 메서드 레벨 보안
# Spring Security의 메서드 레벨 권한 제어
# 특정 API에 역할별 권한 걸기 (예: ADMIN만 가능)
이미 User 엔티티에 Role enum이 있고 (USER, ADMIN), </br>
CustomUserDetails.getAuthorities() 에서 ROLE_USER, ROLE_ADMIN 으로 잘 변환해주고 있음

## 메서드 단위 권한 체크 켜기
먼저 메서드에 @PreAuthorize 같은걸 사용할 수 있게 설정부터 키자
- SecurityConfig 위에 어노테이션 하나 추가 `@EnableMethodSecurity`

```java
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity
```
이렇게 하면 서비스/컨트롤러 메서드에

```java
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ADMIN')")
```
사용 가능

### 예 : 게시판 삭제는 ADMIN만 가능하게
BoardController.delete 에 ADMIN 제한을 걸어보자
```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')") // ✅ ADMIN만 삭제 가능
public void delete(@PathVariable Long id) {
	boardService.delete(id);
}
```
- 이렇게 하면 :
    - 로그인은 USER/ADMIN 둘 다 가능하지만
    - DELETE /boards/{id} 호출 시에는 ROLE_ADMIN 권한이 없으면 403 Forbidden이 떨어짐

중요 포인트 :
- hasRole('ADMIN) -> 실제로는 ROLE_ADMIN authority를 찾음
- 전에 CustomUserDetails에서 ROLE_ prefix 붙여줬기 떄문에 딱 맞음
- User.createAdmin(...) 같은 걸로 ADMIN 유저를 하나 만들어 두면 테스트 가능

# 로그인한 유저 정보(닉네임, id)를 컨트롤러/서비스에서 꺼내 쓰기
CustomUserDetails를 직접 만들었으니까, 거기서 닉네임/이메일/id 를 다 꺼낼 수 있다는게 장점</br>
## 컨트롤러에서 가져오기 (@AuthenticationPrincipal)
예를 들어, 로그인한 유저의 게시글을 조회하는 API를 만든다 했을 때
```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@GetMapping("/me/posts")
public Page<PostResponse.Read> myPosts(
	@AuthenticationPrincipal CustomUserDetails userDetails,
	@PageableDefault(size = 10) Pageable pageable
) {
	String myNickname = userDetails.getNickname(); // ✅ 로그인한 사람 닉네임
	return postService.readByWriter(myNickname, pageable);
}
```
- `@AuthenticationPrincipal CustomUserDetails userDetails`
    - 현재 로그인한 사용자의 CustomUserDetails 객체가 들어온다
- 거기에서 :
    - userDetails.getId() -> DB PK
    - userDetails.getNickname() -> 로그인 아이디(닉네임)
    - userDetails.getEmail() -> 이메일
      다 꺼내 쓸 수 있음

## 서비스에서 가져오기 (SecurityContextHolder)
서비스 계층에서 바로 인증 정보를 꺼내고 싶다면 :
```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public void someServiceLogic() {
	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	Object principal = auth.getPrincipal();

	if (principal instanceof CustomUserDetails userDetails) {
		Long userId = userDetails.getId();
		String nickname = userDetails.getNickname();
		// 여기서 userId, nickname 기반 비즈니스 로직 수행
	}
}
```
- 이 방식은 어디서든(서비스, 유틸 등) 사용할 수 있지만,
- 테스트/유지보수를 생각하면 컨트롤러에서 `@AuthenticationPrincipal`로 받아서 서비스에 파라미터로 넘겨주는 방식이 더 깔끔할 때가 많다

# 여기까지 한 것
지금까지 정리된 상태
1. User 엔티티/DB + Spring Security 폼 로그인
2. ROLE 기반 권한 제어 (`@PreAuthorize("hasROle('Admin'))`)
3. CustomUserDetails 기반으로 로그인 유저 정보 꺼내 쓰기




## 질문 : 근데 boardService에서 user의 등급을 보고 처리해도 되지 않나? 굳이 이렇게 해주는 이유가 있을까?
- boardService에서 role을 확인하고 처리해도 됨
- 다만 권한 체크를 어디서 해줄지에 따라 책임이 달라지고, Spring Security 기능을 쓰면 얻는 이점이 커서 보통 거기서 함

#### 서비스에서 직접 role 체크하는 방식
```java
public void deleteBoard(Long id, User currentUser) {
	if (!currentUser.isAdmin()) {
		throw new ForbiddenException("관리자만 삭제 가능");
	}
	// 삭제 로직...
}
```
이 방식의 장점 :
- 직관적이고 눈에 바로 들어옴
- 도메인 규칙을 코드로 직접 표현하기 쉬움 </br>
  단점/ 한계 :
- 매번 직접 체크해야함
    - 삭제, 수정, 생성 등 권한이 걸리는 모든 메서드에서
        - user 가져오고
        - role 가져오고
        - 예외 던지고
    - 실수로 한 군데 빼먹으면 그 API는 뚫림
- 중복/산재
    - 역할별 접근 권한 로직이 여러 서비스/컨트롤러에 중복되어 흩어짐
    - 나중에 "ADMIN" 말고 MANAGER 도 허용하자 같은 요구가 생기면 관련된 모든 if 문을 찾아다니며 수정해야함
- 기술적 보안 기능과 도메인 로직이 섞임
    - 서비스는 원래 "게시판 삭제, 글 등록" 같은 도메인 규칙에 집중하는게 좋음
    - "이 요청이 인증됐냐, 인가됐냐"는 보안/인프라 레벨의 관심사에 더 가까움

### @PreAuthorize / Security 설정에서 처리하는 방식
예:
```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public void delete(@PathVariable Long id) {
	boardService.delete(id);
}
```
장점 :
1. 보안 규칙이 한눈에 보임
    - 컨트롤러/서비스 메서드 헤더만 봐도
    - "이건 ADMIN만 가능" 같은 규칙을 바로 읽을 수 있음
2. 일관된 처리
    - 인증 안됐으면 401, 권한 없으면 403 등 HTTP 응답 코드를 Spring Security가 알아서 맞게 내줌
    - 예외 처리/로그 등도 공통적으로 묶기 쉬움
3. 중앙집중/선언적(Declarative)
    - SecurityConfig, @PreAuthorize 표현식 하나만 수정하면
    - 여러 API의 권한 정책을 한 번에 바꾸기 쉬움
    - 예 : `"hasRole('ADMIN')" → "hasAnyRole('ADMIN', 'MANAGER')"`로 한 줄 수정.
4. 서비스는 "비즈니스 로직"에만 집중 가능
    - BoardService 입장에서는
    - "여기까지 들어온 호출은 이미 '권한이 있는' 사용자다"라고 가정하고 순수 도메인 로직만 작성하면 됨

### 그럼 어떤게 정답?
실무에서는 둘을 섞어 쓰는 경우가 많음
- "이 API에 접근할 수 있는지" (경비 아저씨 역할)
    - Spring Security / @PreAuthorize / URL 메서드 보안
- "도메인 규칙상 이 유저가 이 리소스를 수정할  수 있는지"
    - 예 : 자기 글만 수정 가능, 관리자는 예외적으로 가능
        - 서비스 내부에서 현재 유저 정보 + 도메인 상태 보고 판단

예를 들어 :
```java
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@DeleteMapping("/posts/{id}")
public void deletePost(@PathVariable Long id,
											 @AuthenticationPrincipal CustomUserDetails principal) {
	postService.deletePost(id, principal.getId());
}
```
서비스쪽 :
```java
public void deletePost(Long postId, Long currentUserId) {
	Post post = postRepository.findById(postId)
		.orElseThrow(...);

	User currentUser = userRepository.findById(currentUserId)
		.orElseThrow(...);

	// 도메인 규칙: 작성자 본인 or ADMIN만 삭제 가능
	if (!post.getUser().getId().equals(currentUserId) && !currentUser.isAdmin()) {
		throw new ForbiddenException("본인 글 또는 관리자만 삭제 가능");
	}

	postRepository.delete(post);
}
```
- Spring Security : 로그인 여부, 기본 역할 체크
- 서비스 : "이 글의 주인과 현재 유저의 관계" 같은 도메인 규칙


### 만들면서 느낀 점 :
- 컨트롤러 단에서 미리 악용될 만한 기능들에는 제한을 걸어 서비스까지 접근 못하게 해서 좋은 것 같음
- 권한이 안된다면 서비스까지 가지 못하니 성능이 더 좋을거라 예상 됨
- 권한에 대해 만들면서 commentService에 deleteAllByUser라는 메서드(지금은 deleteAllByAdmin으로 변경)에 대해
    생각해볼 수 있어서 좋았음 악의적으로 사용할 수 있는 기능들에 대해 생각 할 수 있었음


# Spring Security 세션 기반 로그인 
Spring Security는 스프링 기반 애플리케이션의 인증(Authentication)과 인가(Authorization)를 처리하는 강력한 보안 프레임워크 </br>
세션 기반 로그인은 웹에서 가장 전통적이고 널리 쓰이는 인증 방식이기도 함

## 1. Spring Security 인증 구조 개념
Spring Security는 다음 3가지를 중심으로 동작 : 
### ✔ 1) Authentication(인증)
"이 사용자가 진짜 누구인지 확인하는 과정"
- 로그인 시 전달 받은 username/password가 DB의 유저와 일치하는지 확인하는 단계

### ✔ 2) Authorization (인가)
"이 사용자가 요청한 자원(페이지/기능)을 사용할 권리가 있는지 확인하는 과정"

예: ROLE_ADMIN만 게시판 삭제 가능

### ✔ 3) SecurityContext + Session (세션 저장)
인증에 성공하면 Spring Security는 인증 정보를 Security Context에 저장하고 이 SecurityContext는 다시 세션에 저장
</br>
그래서 인증 이후에는 세션을 통해 로그인 상태가 유지되는 구조

## 2. Spring Security 세션 로그인 전체 흐름
```java
[로그인 요청]
    ↓
UsernamePasswordAuthenticationFilter
    ↓
AuthenticationManager
    ↓
UserDetailsService (내가 구현)
    ↓
UserDetails (내가 구현한 User 정보 객체)
    ↓
인증 성공 → SecurityContext 저장 → 세션 저장
    ↓
클라이언트는 이후 JSESSIONID 쿠키를 통해 인증된 사용자로 인정됨
```
즉, 로그인 성공하면 세션에 인증 정보(SecurityContext)가 저장되고 </br>
브라우저가 JSESSIONID 쿠키를 자동으로 관리하면서 로그인 상태 유지되는 구조

## 3. 세션 기반 인증이란?
### 🔶 핵심 요약
- 인증 성공하면 세션에 사용자 정보가 저장
- 클라이언트는 JSESSIONID 쿠키를 계속 보내며 로그인 상태 유지 
- 서버는 세션 저장소(메모리, Redis 등)에 세션을 저장

### 🔶특징
| 항목           | 설명                     |
|--------------|------------------------|
| 상태(Stateful) | 서버가 세션을 직접 저장함         |
| 서버 부하        | 유저 수가 많아지면 세션 저장 공간 필요 |
| 보안 수준        | 서버 관리에 따라 안정적          |
| 모바일, SPA     | 토큰 기반보다 불편할 수 있음       |

## 4. Security에서 로그인 인증 과정 상제
로그인 시 /login POST 요청이 들어오면 `UsernamePasswordAuthenticationFilter`가 자동으로 동작

### 1) UsernamePasswordAuthenticationFilter
- 로그인  폼에서 username / password 추출 (여기서 username은 로그인 시 사용되는 ID)
- Authentication 객체 생성하여 AuthenticationManager에게 전달
#### 추가 
- Spring Security가 기본 구현체를 자동으로 제공
- SecurityConfig에서 `formLogin()`을 쓰는 순간 SpringSecurity가 자동으로 UsernamePasswordAuthenticationFilter를 등록해서 사용

### 2) AuthenticationManager
- 적절한 AuthenticationProvider 선택
- 기본은 DaoAuthenticationProvider

#### 추가 : AuthenticationManager도 만들지 않았지만 왜 되는걸까?
스프링 부트는 다음 조건이 있으면 AuthenticationManager를 자동 구성 해줌 : 
##### ✔ UserDetailsService 구현체를 제공함
##### ✔ PasswordEncoder 빈을 제공함 
- 이 두개가 있다면 Spring Boot Security는 자동으로 : 
  - DaoAuthenticationProvider 등록
  - AuthenticationManager 생성
  - 이것을 SecurityFilterChain에 연결

### 3) UserDetailsService (개발자가 구현)
```java
package me.boardApp.domain.user;

import lombok.RequiredArgsConstructor;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	// username email 사용(로그인 ID를 말하는 것임)
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// username에는 /login 폼에서 입력한 값이 들어옴
		User user = userRepository.findByEmail(username)
			.orElseThrow(()-> new UserException(ExceptionCode.NOT_FOUND_USER));

		return new CustomUserDetails(user);
	}
}

```
### 4) PasswordEncoder
DB password 비교할 때 반드시 필요

### 5) 인증 성공
- SecurityContext에 Authentication 저장
- Session에 SecurityContext 저장
- JSESSIONID 쿠키 발행

이제 로그인 유지!

## 5. Security 기본 설정 예시 
스프링 6 기준 : 
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/signup").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/home", true)
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login")
        );

    return http.build();
}
```
나는 이렇게 했다 
```java
@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable) // 일단 개발/테스트용으로 csrf 끄기
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/", "/css/**", "/js/**", "/images/**",
					"/h2-console/**", "/users/join", // 회원가입 API는 인증 없이 허용
					"/users/join-form") // 폼 회원가입 처리
					.permitAll() // 이 URL들은 누구나 접근 가능
					.anyRequest().authenticated() // 나머지는 로그인 필요
				)
			.formLogin(form -> form
			// 기본 로그인 폼 사용
				// .loginPage("/login") loginPage 지정 안하면, Spring 기본 로그인 페이지(/login) 자동 제공
				.loginPage("/login")
				.defaultSuccessUrl("/boards", true)
				.permitAll())
			.logout(Customizer.withDefaults()); // 로그인 성공 시 /boards 로 강제 리다이렉트

		return http.build();
	}
```
## 6. 세션 기반 인증의 장점 & 단점
### ✔ 장점
- 구현 간단
- 스프링이 거의 다 자동 처리
- 서버가 인증 상태를 직접 관리 -> 안정적
- 전통적인 웹 서비스에서는 여전히 가장 많이 사용

### ✔ 단점 
- 서버 메모리 사용 증가
- 서버가 여러 대일 때 세션 공유 필요 -> Redis 등 필요
- 모바일/SPA 환경에서는 불편 (토큰이 더 적합)

## 7. JWT와 세션의 차이
| 구분       | 세션           | JWT                    |
|----------|--------------|------------------------|
| 인증 정보 저장 | 서버 메모리/Redis | 클라이언트(LocalStorage-쿠키) |
| 서버 확장성   | 세션 공유 필요     | 확장 쉬움                  |
| 보안       | 서버가 관리 -> 안정 | 탈취되면 위험                |
| 로그아웃     | 서버에서 세션 제거   | 블랙리스트 필요               |
| 적합한 서비스  | 웹 기반         | 모바일·API 서버             |

## ✅ 마무리
**Spring Security 기본 로그인은 세션 기반 구조이며, 인증 성공 시 SecurityContext가 세션에 저장되고 JSESSIONID로 상태가 유지되는 방식이다.</br>**
스프링이 거의 모든 인증 절차를 자동 처리해주므로, 개발자는 UserDetailsService 구현과 Security 설정만 해주면 동작한다
