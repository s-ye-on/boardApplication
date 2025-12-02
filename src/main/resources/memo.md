## annotation

### `@NoArgsConstructor`
- 매개 변수 없는 기본 생성자 만들어줌

### `@AllArgsConstructor`
- 모든 필드를 매개 변수로 받는 생성자 만들어줌

### `@RequiredArgsConstructor`
- `final` 또는 `NonNull` 붙은 필수 필드만 받는 생성자 만들어줌


# 0927
## CommentService 리팩터링
## UserService 완성


# 0928
## Service 리팩터링
- 각각의 서비스는 각자의 repository만 참조하게 리팩터링함
- 여기서 순환참조 문제 발생
- postService를 만들기 위해 boardService가 존재해야함
- 근데 boardService도 존재하려면 postService가 존재해야함
- -> 순환 반복

- 순환 참조되는 설계를 지양해야함
- 설계 문제
- 서로 인터페이스에 의존하게 해서 순환참조 문제 해결
- port 인터페이스 생성


// 1. Optional 그대로 반환 (유연성 제공)
public Optional<User> findByNickname(String nickname) {
return userRepository.findByNickname(nickname);
}

// 2. 없으면 예외 던지는 버전 (자주 쓰는 경우)
public User readByNickname(String nickname) {
return userRepository.findByNickname(nickname)
.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
}

- 이렇게 하면 Optional을 반환하는쪽을 사용하면 다른 곳에서 사용하여
- 예외 메세지를 원하는대로 사용하는쪽에서 반환할 수 있음

### 더 고민해봐야할 것
- user 삭제 시 soft delete로 구현을 했는데
- 에타처럼 user 삭제해도 글과 댓글이 보이게 두고 싶음
- 그래서 User 엔티티에 있는 posts, comments 컬렉션의 CascadeType, orphanRemoval을 빼줌

### 0930
- 스터디 이후 고민해봐야할 것
- dto 리팩터링 
- sealed interface + record class
- Service의 설계 문제
- 보통 순환 참조가 발생하면 설계(의존)가 잘못된거라 하셨음

### @Controller vs @RestController
| 구분         | @Controller                         | @RestController                   |
|------------|-------------------------------------|-----------------------------------|
| 역할         | View(화면)반환용                         | JSON/ 문자열 같은 데이터 응답용(API)         |
| 리턴 값 처리    | ViewResolver를 통해 HTML, JSP 등 뷰로 렌더링 | 리턴 값을 HTTP 응답 본문(body)에 바로 씀      |
| 내부 구성      | 순수 컨트롤러 + @ResponseBody 없음          | @Controller + @ResponseBody 합친 형태 |
| 주로 사용 되는 곳 | 타임 리프, JSP, MVC 기반 웹                | Rest API 서버, 프론트엔드 분리 구조          |

### @Controller
```java
@Controller
public class PageController {

    @GetMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("name", "승연");
        return "hello";  // → templates/hello.html 렌더링
    }
}
```
- 서버는 "hello.html"파일을 찾아서 렌더링한 뒤 HTML을 브라우저로 보냄
- Thymeleaf 같은 템플릿 엔진이 이때 작동

### @RestController
```java
@RestController
public class ApiController {

    @GetMapping("/hello")
    public String hello() {
        return "안녕, 승연!";
    }
}
```
- HTML을 렌더링하지 않고 "안녕, 승연!" 이라는 문자열이 그대로 응답 본문(body)에 들어가서 클라이언트에 전송됨
- JSON 응답도 마찬가지
```java
@RestController
public class ApiController {

    @GetMapping("/user")
    public User getUser() {
        return new User("승연", 24);
    }
}
```
응답
```json
{
  "name": "승연",
  "age": 24
}
```
### 정리
HTML 페이지를 직접 렌더링할 때(Thymeleaf 등) 
    - 추천 어노테이션 : @Controller

JSON, 문자열 등 데이터를 내려주는 API 서버 
    - 추천 어노테이션 : @RestController

만약 @Controller를 쓰는데 일부 메서드만 JSON으로 응답하고 싶으면 그 메서드에만 @ResponseBody 붙이면 됨

- @Controller -> "화면을 보여주는 컨트롤러"
- @RestController -> "데이터(API)를 응답하는 컨트롤러"




임시
```java
IdempotencyRecord record = existing.get();

switch(record.getProgressStatus()){
	/*이미 처리 완료된 요청의 경우 */
  case COMPLETED -> {
		response.setStatus(record.getStatusCode().value());
		response.getWriter().write(record.getREsponseBody() != null ? record.getResponseBody() : "");
		retrun false;
  }
	case PROCESSING -> {
		response.setStatus(HttpServletResponse.SC_CONFLICT);
		return false;
  }
  }
	return false;
```