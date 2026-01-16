# Spring Security 연동 아키텍처 정리

## 1. 인증 방식 개요

- **로그인 방식**: 이메일 + 비밀번호 기반 폼 로그인 (Spring Security `formLogin` 사용)
- **인증 상태 유지**: Spring Security 세션 기반 (서버 세션 + JSESSIONID 쿠키)
- **사용자 정보 소스**: `users` 테이블을 사용하는 `User` 엔티티 (`me.boardApp.domain.user.User`)
- **비밀번호 암호화**: `BCryptPasswordEncoder` (`PasswordEncoder` 빈)

요약하면, **Spring Security가 제공하는 세션 기반 인증 흐름을 사용하되, 사용자 정보와 비밀번호 검증은 직접 만든 `User` 엔티티/Repository를 사용**한다.

---

## 2. 도메인 모델과 User 엔티티

### User 엔티티 (`me.boardApp.domain.user.User`)

- 테이블: `users`
- 주요 필드
  - `Long id` (PK)
  - `String realName` (`@Column(nullable = false, length = 50)`)
  - `String nickname` (`@Column(nullable = false, length = 50, unique = true)`)
  - `String password` (`@Column(nullable = false, length = 100)`, BCrypt 해시 저장)
  - `String email` (`@Column(nullable = false, length = 50, unique = true)`) → **로그인 아이디**
  - `Status status` (`ACTIVATION` / `INACTIVATION`) – soft delete용
  - `Role role` (`USER` / `ADMIN`)
- 주요 도메인 메서드 예시
  - `validatePassword(String raw, PasswordEncoder encoder)`
  - `validateEmail(String email)`
  - `validateRealName(String realName)`
  - `updateNickname(String newNickname)` / `updateEmail(String email)` / `updatePassword(String pw)`
  - `inactivate()` / `activate(String nickname)`
  - `boolean isAdmin()`

이 엔티티는 **비즈니스/도메인 관점의 사용자 정보와 검증 로직**을 책임진다.

---

## 3. Spring Security 설정 구조

### 3-1. `SecurityConfig` (`me.boardApp.config.SecurityConfig`)

```java
@Configuration
@EnableMethodSecurity // @PreAuthorize 등 메서드 단위 권한 체크 활성화
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/css/**", "/js/**", "/images/**",
                    "/h2-console/**",
                    "/users/join",      // 회원가입 API/폼
                    "/users/login",     // (필요 시 API 로그인)
                    "/login"            // 로그인 페이지
                ).permitAll()
                .anyRequest().authenticated() // 나머지 요청은 인증 필요
            )
            .formLogin(form -> form
                .loginPage("/login")              // 커스텀 로그인 페이지 (기본 폼 쓰면 생략 가능)
                .defaultSuccessUrl("/boards", true) // 로그인 성공 시 /boards 로 이동
                .permitAll()
            )
            .logout(Customizer.withDefaults());

        return http.build();
    }
}
```

포인트:

- CSRF는 일단 개발 편의를 위해 `disable()` (실서비스에서는 재검토 필요)
- `/users/join`, `/login` 등은 인증 없이 접근 허용
- 그 외 모든 API는 인증 필요 → 로그인 후에만 접근 가능
- `@EnableMethodSecurity` 로 컨트롤러/서비스 메서드에서 `@PreAuthorize` 사용 가능

### 3-2. `CustomUserDetails` (`me.boardApp.domain.user.CustomUserDetails`)

```java
public class CustomUserDetails implements UserDetails {

    private final User user;

    public Long getId() { return user.getId(); }
    public String getNickname() { return user.getNickname(); }
    public String getEmail() { return user.getEmail(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = "ROLE_" + user.getRole().name();
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // ✅ 로그인 ID = 이메일
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == User.Status.ACTIVATION;
    }
}
```

역할:

- Spring Security 내부에서 사용하는 **인증 주체(Principal)**
- User 엔티티를 보안 관점에서 필요한 정보(아이디, 비밀번호, 권한, 활성여부 등)로 감싸는 어댑터
- 컨트롤러/서비스에서 `@AuthenticationPrincipal CustomUserDetails userDetails` 로 현재 로그인 유저의 `id`, `email`, `nickname` 등을 가져다 쓴다.

### 3-3. `CustomUserDetailsService` (`me.boardApp.domain.user.CustomUserDetailsService`)

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username = 로그인 폼에서 입력한 이메일
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return new CustomUserDetails(user);
    }
}
```

역할:

- Spring Security 가 로그인 시 호출하는 **사용자 조회 어댑터**
- `username`(이메일)을 받아서 `UserRepository.findByEmail` 로 `User` 엔티티를 로딩하고, `CustomUserDetails` 로 감싸서 반환한다.

---

## 4. User 관련 기능과 Spring Security 연동

회원 관련 기능은 `UserController` / `UserService`에서 처리하며, **로그인이 필요한 작업은 항상 `@AuthenticationPrincipal` 로 현재 로그인 유저를 받아서 처리**한다.

### 4-1. 회원가입 (비로그인)

- DTO: `UserRequest.Create`
- 컨트롤러 (예시):

```java
@PostMapping("/users/join")
@ResponseStatus(HttpStatus.CREATED)
public void join(@RequestBody @Valid UserRequest.Create request) {
    userService.join(request);
}
```

- 서비스: 비밀번호를 `passwordEncoder.encode`로 암호화 후 `User` 생성 및 저장.

### 4-2. 로그인 (이메일 + 비밀번호)

두 가지 케이스를 지원 가능:

1. **Spring Security 폼 로그인** (`/login`)
   - 브라우저에서 `username` 입력란에 이메일, `password` 입력란에 비밀번호 입력
   - Spring Security 내부적으로 `loadUserByUsername(email)` → 세션에 인증 정보 저장

2. **REST API 로그인** (`POST /users/login` – 선택사항)
   - DTO: `UserRequest.Login` (email, password)
   - 서비스에서 `userRepository.findByEmail` + `validatePassword` 후 `UserResponse.Login` 반환

### 4-3. 닉네임 / 이메일 / 비밀번호 변경 (로그인 필수)

원칙:

- **누가 수정하는지는 `@AuthenticationPrincipal` 로만 판단**
- DTO 에서는 현재 사용자 식별자(`nickname`, `email`)를 받지 않고,
  - 새 값 + 비밀번호(재입력) 정도만 받는다.

예시) 닉네임 변경:

```java
// DTO
record UpdateNickname(
    @NotBlank
    @Size(min = 3, max = 30)
    String newNickName,

    @NotBlank
    String password
) implements UserRequest {}

// Controller
@PatchMapping("/users/nickname")
@PreAuthorize("hasRole('USER')")
public void updateNickname(
        @RequestBody @ Valid UserRequest.UpdateNickname request,
        @AuthenticationPrincipal CustomUserDetails userDetails
) {
    userService.updateNickname(request, userDetails.getId());
}

// Service
public void updateNickname(UserRequest.UpdateNickname request, Long currentUserId) {
    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

    currentUser.validatePassword(request.password(), passwordEncoder);

    if (userRepository.existsByNickname(request.newNickName())) {
        throw new UserException(ExceptionCode.DUPLICATE_NICKNAME);
    }

    currentUser.updateNickname(request.newNickName());
}
```

이와 같은 패턴으로 `UpdateEmail`, `UpdatePassword`, `Delete(탈퇴)` 등을 모두
`@AuthenticationPrincipal` + 현재 유저 기준으로 처리하도록 리팩터링했다.

---

## 5. 도메인 권한(Authorization) 설계

### 5-1. 역할(Role)

- `User.Role` enum: `USER`, `ADMIN`
- `CustomUserDetails.getAuthorities()` 에서 `ROLE_USER`, `ROLE_ADMIN` 으로 변환

### 5-2. 메서드 단위 권한 체크 (`@PreAuthorize`)

`SecurityConfig` 에 `@EnableMethodSecurity` 를 추가했기 때문에, 컨트롤러/서비스에서 다음과 같이 사용한다.

예시) 게시판 삭제는 관리자만:

```java
@DeleteMapping("/boards/{id}")
@PreAuthorize("hasRole('ADMIN')")
public void deleteBoard(@PathVariable Long id) {
    boardService.delete(id);
}
```

예시) 댓글 수정은 작성자 본인만:

```java
@PatchMapping("/comments/{id}")
@PreAuthorize("hasRole('USER')")
public CommentResponse.Update updateComment(
        @PathVariable Long id,
        @RequestBody @Valid CommentRequest.Update request,
        @AuthenticationPrincipal CustomUserDetails userDetails
) {
    return commentService.updateComment(id, request, userDetails.getId());
}
```

### 5-3. AuthorizationService (도메인 권한 체크)

Spring Security는 **어떤 ROLE 이 요청을 보낼 수 있는지**를 필터/애너테이션에서 체크하고,
구체적으로 “이 자원이 이 유저의 것인지”, “관리자도 허용할지” 같은 도메인 규칙은
별도의 `AuthorizationService` 에서 처리한다.

```java
@Service
public class AuthorizationService {

    public void checkOwner(User resourceOwner, User currentUser) {
        boolean isOwner = resourceOwner.getId().equals(currentUser.getId());
        if (!isOwner) {
            throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
        }
    }

    public void checkOwnerOrAdmin(User resourceOwner, User currentUser) {
        boolean isOwner = resourceOwner.getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.isAdmin();
        if (!isOwner && !isAdmin) {
            throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
        }
    }
}
```

예시) 댓글 삭제 (작성자 본인 or 관리자):

```java
public void deleteByCommentId(Long id, CommentRequest.Delete request, Long currentUserId) {
    Comment target = commentRepository.findById(id)
        .orElseThrow(() -> new CommentException(ExceptionCode.NOT_FOUND_COMMENT));

    User writer = target.getUser();
    User currentUser = commonService.getUserById(currentUserId);

    authorizationService.checkOwnerOrAdmin(writer, currentUser);
    currentUser.validatePassword(request.password(), passwordEncoder);

    // 양방향 연관관계 정리 (orphanRemoval에 의해 DELETE 수행)
    target.getPost().getComments().remove(target);
    target.getUser().getComments().remove(target);
}
```

정리하면:

- **컨트롤러/`@PreAuthorize`**: 어떤 ROLE 이 이 API를 호출할 수 있는지 제어
- **`AuthorizationService` + 도메인 메서드**: 자원 소유자/관리자 여부, 비즈니스 규칙에 따른 인가 로직 담당

---

## 6. 현재 상태 요약

- 이메일 + 비밀번호 기반 Spring Security 폼 로그인 완성
- `User` 엔티티 + `UserRepository` + `CustomUserDetailsService` 로 **DB 기반 인증** 구현
- `CustomUserDetails` 로 현재 로그인 유저의 `id`, `email`, `nickname`, `role`, `status` 접근 가능
- `@AuthenticationPrincipal` + `@PreAuthorize` + `AuthorizationService` 조합으로
  - 게시글/댓글/회원 기능에 대해
  - **작성자 본인/관리자 권한**을 명확하게 제어
- 회원 관련 변경/삭제 API 들은 모두 "현재 로그인한 유저" 기준으로 동작하도록 리팩터링 완료

다음 단계로는 이 구조를 바탕으로, 세션 대신 **JWT 기반 인증/인가로 전환하는 설계**를 진행할 수 있다.
