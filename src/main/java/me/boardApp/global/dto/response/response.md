### ResponseEntity
- ResponseEntity는 Spring Framework에서 이미 제공하는 클래스
- `org.spring.framework.http.ResponseEntity`에 정의 되어 있음
- HTTP 응답 전체를 커스터마이징할 수 있게 해줌


즉 "클라이언트에게 어떤 HTTP 상태 코드(status), 헤더(headers), 본문(body)를 보낼지"를 직접 정할 수 있게 하는 일종의 응답 포장(wrapper) 클래스

```java
return ResponseEntity
    .status(HttpStatus.OK)
    .body("로그인 성공");
```
- 이건 HTTP 200 응답 코드에 "로그인 성공" 이라는 본무을 담아서 보낸다는 뜻

### .status() 와 .body()의 의미
`.status(HttpStatus.XXX)`
- HTTP 응답 코드 (예 : 200 OK, 400 BAD_REQUEST, 404 NOT_FOUND 등)을 지정

`.body(Object body)`
- 실제 응답 본문에 담을 객체나 메시지를 지정
- 문자열일 수도 있고, JSON으로 변환될 DTO 객체일 수도 있음

```java
return ResponseEntity
    .status(HttpStatus.OK)
    .body(new LoginResponse("로그인 성공", userId));
```
이렇게 하면 JSON 형태로 아래처럼 응답됨
```json
{
  "message": "로그인 성공",
  "userId": 123
}
```
즉, body에는 단순 문자열뿐 아니라 DTO도 올 수 있음

### response 패키지의 위치 
지금 global/dto/response 도 구조상 괜찮긴 함

하지만 조금 더 명확하게 의도를 구분하고 싶다면
- global/exception/
- global/response/
- global/dto/

즉 response는 전역 응답 처리용 (공통 ResponseEntity, Result 구조 등)
dto는 요청/응답 시 각각의 세부 데이터 구조로 분리하는 느낌

그래서 response는 "전역 응답 틀"이고 각 도메인의 DTO들은 user/dto/, auth/dto/ 같은 쪽에 넣는게 깔끔

### 실패 응답은 ResponseEntity로 던질 수 있을까? 
서비스에서는 ResponseEntity를 던지면 안됨
이유 : 
- ResponseEntity는 컨트롤러 단에서 HTTP 응답을 만들 때 사용하는 것이고
- Service는 비즈니스 로직만 담당해야 함

즉,
- Service에서는 예외(Exception)을 던지고
- Controller나 전역 예외 처리기(GlobalExceptionHandler) 에서 그 예외를 잡아서 ResponseEntity로 변환해야 함

예
- Service
```java
public User login(String id, String password) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new InvalidLoginException("아이디 또는 비밀번호가 잘못되었습니다."));
    return user;
}
```
- GlobalExceptionHandler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<CommonResponse> handleInvalidLogin(InvalidLoginException e) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new CommonResponse(e.getMessage(), false));
    }
}
```
- Controller
```java
@PostMapping("/login")
public ResponseEntity<CommonResponse> login(@RequestBody LoginRequest request) {
    userService.login(request.getId(), request.getPassword());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(new CommonResponse("로그인 성공", true));
}
```
이렇게 하면 성공/실패 응답을 Controller + GlobalHandler에서 통일되게 관리할 수 있음