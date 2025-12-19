# 이미 UserService, UserRepository가 존재하는데 
# 왜 CustomUserDetails / UserDetailsService가 필요한가?

Spring Security를 적용하다 보면  
이미 `UserService` , `UserRepository` 가 있는데도  
왜 `UserDetails` , `UserDetailsService`를 또 만들어야 하는지 헷갈렸다  

이 글에서는  
**Spring Security의 인증 흐름 관점에서**  
**User Details / UserDetailsService 가 왜 필요한지** 를 정리해본다

---

## Spring Security는 User 엔티티를 모른다 
Spring Security는  
`User` 엔티티나 `UserService`를 직접 알지 못한다.  

Spring Security가 아는 것은 오직 이것뿐이다
> "아이디를 줄테니",
> 이 사람의 비밀번호 / 권한 /상태 정보를
> **UserDetails 형태로 돌려달라**

이 계약을 담당하는 인터페이스가 바로 : 
- `UserDetails`
- `UserDetailsService`

---

## Spring Security 인증 흐름에서의 위치 
폼 로그인 기준으로 인증 흐름은 다음과 같다  

1. 사용자가 `/login` 에서 아이디 / 비밀번호 전송
2. `UsernamePasswordAuthenticationFilter` 가 요청을 가로챔 
3. `AuthenticationManager` 에게 인증 요청 전달
4. `AuthenticationProvider`가 실제 인증 수행
5. 이때 사용자 정보를 가져오기 위해 **UserDetailsService 호출**
6. `UserDetails`를 반환 받아 비밀번호 비교
7. 인증 성공 시 `Authentication` 생성
8. `SecurityContextHolder` 에 인증 정보 저장

즉,  
**Spring Security는 로그인 시점에 반드시**    
**UserDetailsService -> UserDetails를 거쳐야한다**  

---

## 각 컴포넌트의 역할 정리 
### UserRepository
- DB 접근 전용
- `findByEmail` , `save` 등 순수 데이터 접근 담당

### UserService
- 비즈니스 로직 담당
- 회원가입, 로그인 응답 DTO 생성, 닉네임 변경, 회원 탈퇴 등
- "게시판 서비스에서 필요한 유저 로직"

### UserDetails
- **Spring Security 관점의 사용자 정보**
- Spring Security 가 인증/인가에 필요한 최소 정보만 정의 : 

```java
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface UserDetails {
	String getUsername(); // 로그인 ID(우리는 이메일로 쓸지, 닉네임으로 쓸지 선택)

	String getPassword(); // 인코딩된 비밀번호

	Collection<? extends GrantedAuthority> getAuthorities(); // 권한(Role) 목록 (ROLE_USER, ROLE_ADMIN 등)

	boolean isAccountNonLocked(); // 계정 잠금 여부 

	boolean isEnabled(); // 활성화 여부 
}
```

#### 내 User 엔티티에는
realName, nickname, email, status, role 등 다양한 필드가 있지만   
Spring Security 는 그 중 일부만 필요하다    

그래서 **User -> UserDetails로 변환해주는 어댑터**가 필요하고     
그 역할을 하는 것이 CustomUserDetails 다

### UserDetailsService
- "아이디(username)를 받아 UserDetails를 반환"하는 인터페이스  
- Spring Security 가 딱 이 메서드만 호출한다 :  

```java
  UserDetails loadUserByUsername(String username);
```
구현체(CustomUserDetailsService)에서는 보통 : 
1. DB에서 User 조회
2. 없으면 예외
3. 있으면 new CustomUserDetails(user) 반환

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

즉,  
Spring Security <-> (UserDetailsService / UserDetails) <-> (UserRepository / User 엔티티) </br>
이렇게 중간 어댑터 계층을 두는 구조.

---

## 왜 UserService를 그대로 쓰지 않고, 굳이 UserDetailsService 인가?
이유는 두가지  

### 1. 스펙이 정해져 있기 때문
- Spring Security 내부는  
UserDetailsService / UserDetails 인터페이스에 맞춰 설계되어 있음  

- `UserService.login()` 같은 메서드를  
Security가 임의로 호출할 수는 없음
- UserService는 내가 직접 정의한 비즈니스 용도라, 메서드 시그니처도, 반환 타입도 전혀 다름

### 2. 관심사의 분리
UserService는  
-> 비즈니스 로직 담당 
    - "회원가입할 때 비밀번호 어떻게 검증할까?"
    - "DTO"로 뭐를 반환할까?
    - "에러 메시지를 어떻게 줄까?" </br>
    이런 도메인/ 비즈니스 로직에 집중. </br>
  

UserDetailsService는  
-> "로그인 시 사용자 정보를 Security가 이해할 수 있게 변환"
    - "로그인 시 아이디로 DB에서 유저를 찾아서, 스프링 시큐리티가 이해할 수 있는 형태(UserDetails)로 넘겨주기"  
딱 이 한가지 역할만 담당  

보안 관점의 역할을  
도메인 서비스에 섞지 않는 것이 더 깔끔한 구조다

---

## CustomUserDetails는 컨트롤러에서 쓰이는데, 왜 CustomUserDetailsService는 안보일까?
### CustomUserDetails
- 인증이 끝난 후의 결과 (Principal)
- 컨트롤러에서 이렇게 사용

```java
@AuthenticationPrincipal CustomUserDetails customUserDetails
```
-> "지금 로그인한 사용자 정보"
"인증이 끝난 후, 현재 로그인한 사용자 정보"


### CustomUserDetailsService
- **로그인 과정 중에만 사용**
- Security 필터 / Provider 내부에서 호출됨 
- 컨트롤러에서 직접 호출할 일 없음

### 핵심 정리
- `CustomUserDetails` 는 **완성된 인증 결과(Principal)** 다
- `CustomUserDetailsService` 는 **로그인 과정에서 그 Principal을 만들어주는 공장** 이다
- 컨트롤러는 "이미 인증된 결과"만 필요하므로 `CustomUserDetails`만 자주 보인다
- 반대로 `CustomUserDetailsService`는 로그인 과정에서만 Security 내부에서 호출되기 때문에  
  우리가 직접 호출할 일이 거의 없다

즉,  
- `CustomuserDetails` = 인증 완료된 결과물
- `CustomUserDetailsService` = 인증 과정의 공장

---

## JWT를 사용하면? 
JWT 기반 인증에서는  
AuthenticationManager / UserDetailsService를 직접 쓰지 않을 수도 있다  
  
하지만 :  
- Spring Security와의 정석적인 연동
- 메서드 보안, 권한 처리  

을 고려하면  
**UserDetails / UserDetailsService 구조를 이해하는 것은 여전히 중요하다.**

---

## 📚마무리 정리
- Spring Security는 User 엔티티를 직접 알지 못한다
- 인증 시 반드시 UserDetails /UserDetailsService를 통해 사용자 정보를 조회한다 
- `CustomUserDetails` 는 "인증 결과"
- `CustomUserDetailsService` 는 "인증 과정에서 결과를 만들어주는 역할" 
- `UserService` 와 `UserDetailsService`는 책임이 다르며, 분리하는 것이 설계적으로 더 깔끔하다
- 즉, UserDetails/UserDetailsService는  
"Spring Security와 내 도메인 사이의 번역기 역할" 을 한다 
