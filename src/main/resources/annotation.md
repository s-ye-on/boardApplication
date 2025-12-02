# 🧩 스프링 예외 처리 관련 어노테이션 정리

## ✅ 1️⃣ @RestController
```java
@RestController
public class UserController {
// ...
}
```

💬 하는 일
- 이 클래스를 HTTP 요청을 처리하는 컨트롤러로 등록함.
- 내부적으로 @Controller + @ResponseBody를 합친 것임.
- 메서드의 반환값이 JSON 혹은 문자열로 직접 응답됨.

💡 언제 쓰냐?
- REST API 서버 (데이터 응답 중심) 만들 때 사용
- 화면 렌더링(@Controller)이 아니라 API 응답용일 때

## ✅ 2️⃣ @RequestMapping("/users")
```java
@RequestMapping("/users")
public class UserController { ... }
```

💬 하는 일
- 이 컨트롤러의 공통 URL prefix를 지정함.
- 예: @PostMapping("/join") → 실제 URL은 /users/join

💡 언제 쓰냐?
- API를 그룹화하고 싶을 때 (예: /users, /posts, /comments)

## ✅ 3️⃣ @PostMapping, @PatchMapping, @DeleteMapping

```java
@PostMapping("/join")
public void joinUser(...) { ... }
```

💬 하는 일
- 특정 HTTP 메서드(POST, PATCH, DELETE 등) 를 처리하는 매핑 어노테이션.
- 내부적으로는 @RequestMapping(method = RequestMethod.POST)과 동일.

`@PostMapping`
새 리소스 생성 (회원가입, 글쓰기 등)

`@GetMapping`
리소스 조회

`@PatchMapping`
일부 수정 (닉네임 변경 등)

`@PutMapping`
전체 수정

`@DeleteMapping`
리소스 삭제

## ✅ 4️⃣ @RequestBody
```java
public void joinUser(@RequestBody UserRequest.Create request) { ... }
```

💬 하는 일
- HTTP 요청 본문(JSON) 을 Java 객체로 변환해줌.


  💡 언제 쓰냐?
- 요청 데이터가 Body(JSON) 로 오는 경우 (회원가입, 로그인 등)

## ✅ 5️⃣ @Valid

```java
public void joinUser(@RequestBody @Valid UserRequest.Create request)
```

💬 하는 일
- DTO의 검증 어노테이션(@NotBlank, @Email 등) 을 자동으로 검사함.
- 유효하지 않으면 MethodArgumentNotValidException 발생 → 예외 처리기로 전달됨.

💡 언제 쓰냐?
- 클라이언트 요청값의 유효성 검사 시

## ✅ 6️⃣ @ResponseStatus(HttpStatus.CREATED)

```java
@ResponseStatus(HttpStatus.CREATED)
public void joinUser(...) { ... }
```

💬 하는 일
- 해당 메서드의 HTTP 응답 상태 코드를 지정함.
- 따로 ResponseEntity 안 써도 201 Created 응답이 반환됨.

💡 언제 쓰냐?
- POST 요청 시 성공 응답 (HttpStatus.CREATED)
- DELETE 요청 성공 시 HttpStatus.NO_CONTENT 등도 자주 사용

## ✅ 7️⃣ @RestControllerAdvice

```java
@RestControllerAdvice
public class ApiExceptionHandler { ... }
```

💬 하는 일
- 전역 예외 처리기 역할
- 모든 컨트롤러에서 발생하는 예외를 한 곳에서 잡음
- @ControllerAdvice + @ResponseBody 조합과 동일

💡 언제 쓰냐?
- 여러 컨트롤러의 공통 예외 처리 로직을 한 곳에서 관리할 때

## ✅ 8️⃣ @ExceptionHandler
```java
@ExceptionHandler(ApiException.class)
public ResponseEntity<?> handleApiException(ApiException e) { ... }
```

💬 하는 일
- 지정된 예외 타입이 발생했을 때 이 메서드 실행
- 예외 객체(e)를 인자로 받아 커스텀 응답 작성 가능

예시 :
throw new ApiException(ExceptionCode.NOT_FOUND_POST);
-> handleApiException() 자동 실행

## ✅ 9️⃣ @ResponseStatus
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
@ExceptionHandler(MethodArgumentNotValidException.class)
public String handleBeanValidationFailed(Exception e) { ... }
```
💬 하는 일
- 예외가 발생했을 때 응답 상태 코드를 지정함
- 간단한 예외 처리에 주로 사용 (ResponseEntity 안 쓸 때)

## ✅ 1️⃣0️⃣ @RequiredArgsConstructor
```java
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;
}
```
💬 하는 일
- final 또는 @NonNull 필드에 대한 생성자를 자동 생성
- 스프링이 해당 생성자를 통해 의존성 주입(DI) 수행함.

```java
// 자동 생성되는 코드
public UserController(UserService userService) {
	this.userService = userService;
}
```
💡 언제 쓰냐?
- 생성자 주입을 간결하고 안전하게 하고 싶을 때
- @Autowired 대신 권장되는 방식

### `@RequestParam` vs `@PathVariable`
`@PathVariable`
- URL 경로 자체에 포함된 값을 매핑해서 가져오는 방식
- 예시 :
```java
@GetMapping("/posts/{postId}/comments/{commentId}")
public CommentReadResponse getComment(
        @PathVariable Long postId,
        @PathVariable Long commentId
) {
    return commentService.readById(postId, commentId);
}
```
요청 예시 : `GET /posts/10/comments/3`
- 여기서 postId = 10, commentId =3이 URL 경로의 일부
- 리소스를 식별할 때 사용
- "어떤 게시글의 몇 번째 댓글인가" 같은 경우에 좋음

`@RequestParam`
- URL의 쿼리스트링(?뒤에 붙는 부분)으로 전달된 값을 가져오는 방식
- 예시 : 
```java
@GetMapping("/comments/search")
public Page<CommentReadResponse> searchComments(
        @RequestParam String writerName,
        @RequestParam(required = false) Long postId
) {
    return commentService.search(writerName, postId);
}
```
요청 예시 : `GET /comments/search?writerName=승연&postId=5`
- 여기서 writerName=승연, postId=5는 검색 조건이나 필터로 쓰는 값
- 리소스 식별보다는 "조회 조건"이 목적일 때 좋음
- `required = false` : 이 요청 파라미터는 없어도 된다는 뜻
- `@RequestParam`은 기본적으로 필수(required = true)
- `@RequestParam(defaultValue = "1") int page` 이런식으로 기본 값 지정으로도 사용 가능