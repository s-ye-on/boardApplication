# 메서드 보안을 어떻게 실무에서 사용할까? 

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

### 그럼 어떤게 정답? 블로그 
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
public void deletePost(
	@PathVariable Long id,
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
- 