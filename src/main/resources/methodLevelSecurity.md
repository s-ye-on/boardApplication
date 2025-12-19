# Spring Security의 메서드 보안 (Method Security)
- `@PreAuthorize` 를 쓰는 이유  

> 메서드 보안은 권한 체크를 코드가 아니라  
> 실행 경계에서 선언적으로 처리하기 위한 전략이다.

## 왜 @PreAuthorize 같은 메서드 보안을 사용할까? 
`@PreAuthorize`는 단순히 "ADMIN만 접근 가능"을 편하게 쓰기 위한 기능이 아니다.  
핵심은 **권한 체크를 비즈니스 로직이 아닌, 실행 경계(Boundary)에서 처리하기 위함** 이다.  

`@PreAuthorize`는 컨트롤러 / 서비스 메서드가 실행되기 *직전*에 평가되며,  
조건을 만족하지 않으면 메서드 로직은 아예 실행되지 않는다. 


## 특정 API에 역할별 권한 걸기 (예: ADMIN만 가능)
이미 User 엔티티에 Role enum이 있고 (USER, ADMIN), </br>
`CustomUserDetails.getAuthorities()` 에서 ROLE_USER, ROLE_ADMIN 으로 잘 변환해주고 있음

### 메서드 단위 권한 체크 켜기
먼저 메서드에 @PreAuthorize 같은걸 사용할 수 있게 설정부터 키자
- SecurityConfig 위에 어노테이션 하나 추가

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
이때 응답 코드가 깔끔하게 분리된다.
- 인증이 안 된 사용자 -> 401 Unauthorized
- 인증은 되었지만 권한이 없는 사용자 -> 403 Forbidden


#### 중요 포인트 : hasRole vs hasAuthority
참고로 `hasRole('ADMIN')`은 내부적으로  
`ROLE_ADMIN`이라는 authority를 찾는다.  

즉, 
- hasRole('ADMIN') -> ROLE_ADMIN
- hasAuthority('ROLE_ADMIN') -> ROLE_ADMIN  

CustomUserDetails에서 ROLE_ prefix를 붙여주고 있다면  
hasRole을 사용하는 방식이 더 읽기 좋다 


### 🙋질문 : 근데 boardService에서 user의 등급을 보고 처리해도 되지 않나? 굳이 이렇게 해주는 이유가 있을까?
- boardService에서 role을 확인하고 처리해도 됨 
- 다만 권한 체크를 어디서 해줄지에 따라 책임이 달라지고, Spring Security 기능을 쓰면 얻는 이점이 커서 보통 거기서 함
- 어노테이션만 수정하면 되기 때문에 변경 범위가 작다

#### Programmatic Authorization (서비스에서 직접 권한 체크 방식)
```java
public void deleteBoard(Long id, User currentUser) {
    if (!currentUser.isAdmin()) {
        throw new ForbiddenException("관리자만 삭제 가능");
    }
    // 삭제 로직...
}
```

### 서비스에서 직접 권한 체크하는 방식의 장점
- 직관적이고 눈에 바로 들어옴
- 도메인 규칙을 코드로 직접 표현하기 쉬움 </br>

### 단점/ 한계
- 매번 직접 체크해야함
    - 삭제, 수정, 생성 등 권한이 걸리는 모든 메서드에서
        - user 가져오고
        - role 가져오고
        - 예외 던지고
    - 실수로 한 군데 빼먹으면 그 API는 뚫린다
- 중복/산재
    - 역할별 접근 권한 로직이 여러 서비스/컨트롤러에 중복되어 흩어짐
    - 나중에 "ADMIN" 말고 MANAGER 도 허용하자 같은 요구가 생기면 관련된 모든걸 수정해야함  
      (모든 if 문을 찾아 수정해야 한다)
    - 기술적 보안 관심사와 도메인 로직이 섞인다
      - 서비스는 원래 "삭제한다 / 등록한다" 같은 비즈니스 로직에 집중하는게 좋다 

### 그럼 어떤게 정답?
실무에서는 둘을 섞어 쓰는 경우가 많다  
#### 1️⃣Spring Security (경비 아저씨 역할)
- "이 API에 접근할 수 있는지"  
    - Spring Security / @PreAuthorize / URL 메서드 보안

#### 2️⃣서비스 계층 (도메인 규칙)
- "도메인 규칙상 이 유저가 이 리소스를 수정할  수 있는가?"
    - 예 : 자기 글만 수정 가능, 관리자는 예외적으로 가능
        - 서비스 내부에서 현재 유저 정보 + 도메인 상태 보고 판단

### `@PreAuthorize` 는 보통 어디에 붙일까?
- 컨트롤러에 붙이는 경우
    - API 단위로 접근 자체를 제한하고 싶을 때
    - REST API의 책임이 명확해짐

- 서비스에 붙이는 경우
    - 동일한 비즈니스 로직을 여러 컨트롤러에서 호출할 때
    - 컨트롤러를 우회한 호출도 함께 보호하고 싶을 때

실무에서는  
"외부 접근 경계"에 가까운 곳에 두는 것이 일반적이다  

### 만들면서 느낀 점 :
- 컨트롤러 단에서 미리 악용될 만한 기능들에는 제한을 걸어 서비스까지 접근 못하게 해서 좋은 것 같음
- 권한이 안된다면 서비스까지 가지 못하니 성능이 더 좋을거라 예상 됨
- 권한에 대해 만들면서 commentService에 deleteAllByUser라는 메서드(지금은 deleteAllByAdmin으로 변경)에 대해
  생각해볼 수 있어서 좋았음 악의적으로 사용할 수 있는 기능들에 대해 생각 할 수 있었음

## 📚 마무리 정리 
- 서비스에서 직접 role을 체크하는 방식은 직관적이지만,  
    중복과 누락 위험이 있다
- `@PreAuthorize`를 사용하는 메서드 보안은  
    권한 체크를 실행 경계에서 선언적으로 처리할 수 있게 해준다 
- 따라서 역할 기반 접근 제어(Role-based Authorization)는  
    Spring Security의 메서드 보안을 활용하는 것이 더 안전하고 유지보수에 유리하다
- 메서드 보안은  
    Filter 기반 인증/인가가 끝난 이후,  
    컨트롤러/서비스 메서드 실행 직전에 동작한다. 

### 다음 글에서는
이 메서드 보안에서 사용한 CustomUserDetails가 어디서 생성되고,  
**왜 필요한지**를 Spring Security 인증 흐름 기준으로 정리해본다