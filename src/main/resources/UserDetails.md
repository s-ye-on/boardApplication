# 이미 UserService, UserRepository가 존재하는데 왜 또 CustomUserDetails / UserDetailsService가 필요한가?

## Spring Security 입장에서 보면
폼 로그인 흐름은 대략 이렇게 흘러감 : 
1. 사용자가 /login 에서 아이디/비번 전송
2. UsernamePasswordAuthenticationFilter 가 이걸 받아서 Authentication 객체 생성
3. 이걸 AuthenticationManager 에게 넘김
4. AuthenticationManager -> 내부의 DaoAuthenticationProvider 가 실제 인증을 처리
5. DaoAuthenticationProvider 가 사용자 정보를 어디서 가져오나? -> UserDetailsService

즉, Spring Security는 </br>
"아이디를 줄 테니, 이 사람의 비밀번호/권한/상태 등을 담은 UserDetails를 돌려줘" 라는 인터페이스만 알고 있음 </br>
그게 바로 : 
- UserDetailsService
- UserDetails

## 각자의 역할 정리
### UserRepository
- DB 접근 전용
- findByEmail, findByNickname save 같은 순수 데이터 엑세스 담당

### UserService
- 이미 만든 비즈니스 로직 담당
  - 회원가입, 로그인 응답 DTO 만들기, 닉네임 변경, 회원 탈퇴 등
- "게시판 서비스에서 필요한 유저 관련 기능들"

### UserDetails
- "Spring Security 관점의 유저 정보" 인터페이스
- Spring Security 가 로그인/인가할 때 꼭 알아야 하는 정보만 정의돼 있음 : 
  - getUsername() : 로그인 ID(우리는 이메일로 쓸지, 닉네임으로 쓸지 선택)
  - getPassword() : 인코딩된 비밀번호
  - getAuthorities() : 권한(Role) 목록 (ROLE_USER, ROLE_ADMIN 등)
  - 계정 만료 여부, 잠금 여부, 활성화 여부 등

#### 내 User 엔티티에는
- realName, nickname, email, status, role 등등 필드가 잔뜩 있는데  
- Spring Security 는 그 중 일부만 필요해.
- 그래서 CustomUserDetails 가 </br>  
“User 엔티티 → UserDetails 형태로 바꿔 주는 어댑터” 역할을 하는 거야.

### UserDetailsService
- "username(아이디)"을 받아서 UserDetails를 돌려주는 서비스" 인터페이스
- Spring Security 가 딱 이 메서드만 호출함 : </br>
```java
  UserDetails loadUserByUsername(String username)
```
- 우리 구현인 CustomUserDetailsService 에서는 내부에서 : </br>
  - UserRepository.findByEmail(username) 호출해서 User 찾고
  - 못찾으면 UsernameNotFoundException 던지고
  - 찾으면 new CustomUserDetails(user) 로 감싸서 리턴
즉, </br>
Spring Security <-> (UserDetailsService / UserDetails) <-> (UserRepository / User 엔티티) </br>
이렇게 중간 어댑터 계층을 두는 구조.

## 왜 UserService를 그대로 쓰지 않고, 굳이 UserDetailsService 인가?
이유는 두가지
### 1. 스펙이 정해져 있음
- Spring Security 내부 코드들은 UserDetailsService / UserDetails 인터페이스에 맞춰 설계되어 있음
- UserService는 내가 직접 정의한 비즈니스 용도라, 메서드 시그니처도, 반환 타입도 전혀 다름
- 스플이이 "마음대로" 내 UserService.login()을 호출해 줄 수 있는 구조가 아님

### 2. 관심사의 분리
UserService는
- "회원가입할 때 비밀번호 어떻게 검증할까?"
- "DTO"로 뭐를 반환할까?
- "에러 메시지를 어떻게 줄까?" </br>
이런 도메인/ 비즈니스 로직에 집중. </br>
</br>
UserDetailsService는
- "로그인 시 아이디로 DB에서 유저를 찾아서, 스프링 시큐리티가 이해할 수 있는 형태(UserDetails)로 넘겨주기" </br>
딱 이 한가지 역할만 담당
- 그래서 보통 UserService를 직접 건드리지 않고, </br>
보안/인증용 어댑터로 UserDetailsService를 따로 두는게 깔끔한 패턴
</br>
물론 CustomuserDetailsService 안에서 이미 있는 UserService를 사용해서 조회하는 방식도 가능함 : 
```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 예: 이메일 기준 조회하는 메서드를 UserService 에 추가해두고
        User user = userService.loadUserByEmail(username);
        return new CustomUserDetails(user);
    }
}
```
하지만 중요한건 "Spring Security는 반드시 UserDetailsService / UserDetails를 통해서만 유저를 본다"는 점

### 결론 : 만들어야 하나? -> "스프링 시큐리티랑 제대로 연동하려면 만들어야한다!"
- 그냥 내가 만든 /users/login API만 쓰고, Spring Security의 /login 폼 로그인을 안쓴다
  - 굳이 UserDetailsService / UserDetails가 필요 없을 수도 있음 (이 경우는 내가 직접 세션/JWT 처리)
- 하지만 지금처럼 /login 폼 + Spring Security 인증/인가 기능을 사용하고 싶다
  - Spring Security가 요구하는 방식대로 UserDetails/UserDetailsService를 제공해야 </br>
    내 User 엔티티/DB와 자연스럽게 연결할 수 있음

## CustomUserDetails는 컨트롤러에서 쓰이는데, 왜 CustomUserDetailsService는 안보일까?
### CustomUserDetails
- 역할 : 
  - "스프링 시큐리티가 이해할 수 있는 사용자 정보" 형태로 감싼 Principal 객체
- 컨트롤러에서 매개 변수에

```java
import me.boardApp.domain.user.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@AuthenticationPrincipal CustomUserDetails customUserDetails
```
이렇게 많이 쓴다
- 이건 이미 인증이 끝난 다음에, "지금 로그인해 있는 사람 정보 좀 줘"할 때 사용하는 것

즉, CustomUserDetails = "인증이 끝난 후, 현재 로그인한 사용자 정보"

### CustomUserDetailsService
```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmail(username)
            .orElseThrow(...);
        return new CustomUserDetails(user);
    }
}
```
- 역할 :
  - "로그인 시점에 이메일/아이디로 유저를 찾아서 `CustomUserDetails`로 감싸주는 어댑터"
- 이건 컨트롤러에서 직접 호출하는 용도가 아니고, 스프링 시큐리티 내부에서 로그인 처리할 때 사용하는 "스프링용 서비스"

핵심 : 
- CustomUserDetails는 "완성된 인증 결과(Principal)"
- CustomUserDetailsService는 "로그인 과정에서 그 Principal을 만들어주는 공장"
- 컨트롤러는 결과만 쓰니까 CustomUserDetails만 보이고, CustomUserDetailsService는 Security 필터/프로바이더 내부에서만 쓰이기에 </br>
    우리가 직접 호출할 일이 잘 없는 것임

### JWT를 사용하면? 
로그인 과정에 AuthenticationManager를 안쓰고, CustomUserDetailsService도 직접 호출 안하고 있음
- "지금 구조에서는 필수는 아님. 하지만 Security의 정석적인 통로" 느낌