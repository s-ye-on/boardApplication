# 10.10

## 예외 처리

### ApiException

- ApiException은 모든 커스텀 예외의 부모 클래스 역할을 함
- RuntimeException을 상속해서, 체크 예외가 아닌 언체크 예외로 사용 가능
- ExceptionCode(enum)를 포함 시켜서, 예외마다 HTTP 상태 코드와 메세지를 관리하게 함

    역할 : 예외 객체 생성(throw용)

### ExceptionCode

- 모든 예외의 종류를 한 곳에서 관리하는 enum
- 각 예외는 HttpStatus와 기본 메시지를 가짐
- 새로운 예외가 필요할 때 여기에 상수만 추가하면 됨

    역할 : 예외 종류/상태/메시지 정의

### ApiExceptionHandler

- `@RestControllerAdvice`는 전역 예외 처리를 담당
- `@ExceptionHandler`로 각 예외 타입별로 핸들링
- `ResponseEntity`로 적절한 HTTP 상태코드 + 메시지 반환
- `ConstraintViolationException`, `MethodArgumentNotValidException`은 주로 @Valid 검증 실패 시 발생

| 상황                                       | 실제 예외                           |
|------------------------------------------|---------------------------------|
| @RequestBody + @Valid                    | MethodArgumentNotValidException |
| @PathVariable, @RequestPara + validation | ConstraintViolationException    |

- 마지막 `RuntimeException` 핸들러는 catch-all, 즉 처리되지 않은 예외를 잡아서 500 반환
- 어디서 사용한다고 등록하지 않아도 자동으로 동작
    
    역할 : 전역에서 던져진 예외를 받아서 응답 형태로 변환

1.	ApiException 발생 →
2.	스프링이 @RestControllerAdvice 스캔 →
3.	@ExceptionHandler(ApiException.class) 가 해당 예외를 처리 →
4.	ResponseEntity.status(code.getStatus()).body(e.getMessage()) 가 반환


### PostException

- 게시글 관련 예외 전용 클래스
- 예 : 게시글이 없을 때 , 권한이 없을 때 등
- throw new PostException(ExceptionCode.NOT_FOUND_POST) 처럼 간단히 사용 가능

### 전체 동작 흐름 요약
1. 서비스 로직에서 예외 발생
2. 예외가 `ApiExceptionHandler`로 전달됨
3. 핸들러가 HTTP 응답 생성
4. 유효성 검증 실패 시
   - @Valid가 붙은 DTO 검증 실패 시  `ConstraintViolationException` 또는
       `MethodArgumentNotValidException` 발생
5. 그 외 예기치 못한 오류는 500으로 처리
   - 예 : `NullPointerException` 등
   - "서버 오류가 발생했습니다." 메시지로 일관되게 응답

### private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

- 로그를 남기기 위한 객체
- 서버가 돌아갈 때 예외가 터지면, 사용자에게는 메시지만 보여주고
개발자는 그 예외 내용을 로그 파일로 확인해야 함
- 그걸 도와주는 것이 Logger

```java
@ExceptionHandler(ApiException.class)
public ResponseEntity<?> handleApiException(ApiException e) {
    log.error("API 예외 발생: {}", e.getMessage(), e); // 이게 바로 로그 남기기
    var code = e.getExceptionCode();

    return ResponseEntity
        .status(code.getStatus())
        .body(e.getMessage());
}
```
- 사용자는 에러 메시지 하나만 받고, 개발자는 서버 로그에서 "어디서 예외가 터졌는지" 추적할 수 있게 됨

### @ExceptionHandler(ApiException.class)
```java
@ExceptionHandler(ApiException.class)
public ResponseEntity<?> handleApiException(ApiException e) {
	var code = e.getExceptionCode();

	return ResponseEntity
		.status(code.getStatus())
		.body(e.getMessage());
}
```
- 역할 : 우리가 만든 커스텀 예외(ApiException)를 처리하는 메서드


```java
throw new ApiException(ExceptionCode.NOT_FOUND_POST);
```
- 코드 어딘가에서 이런식으로 던지면, 스프링은 바로 `handleApiException`을 찾아와서 실행해줌

흐름
1. 예외가 Controller나 Service 안에서 발생함
2. 스프링이 @RestControllerAdvice 붙은 클래스들을 스캔함
3. @ExceptionHandler(ApiException.class)를 발견하고, 해당 메서드를 실행
4. ResponseEntity를 만들어서 클라이언트에게 응답

### @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class)
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
@ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
public String handleBeanValidationFailed(Exception e) {
	return e.getMessage();
}
```
- 역할 : Bean Validation(@Valid) 관련 예외를 처리하는 메서드
- 컨트롤러 메서드 파라미터에 유효성 검사를 통과하지 못하면
`MethodArgumentNotValidExfeption` 같은 예외가 터짐

### @ExceptionHandler(RuntimeException.class)
```java
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
@ExceptionHandler(RuntimeException.class)
public String handleUncaughtException(RuntimeException ex) {
	return ex.getMessage();
}
```
- 그 어떤 핸들러에도 안 잡힌 예외들을 처리하는 "마지막 안전망"
- 우리가 예상하지 못한 예외가 발생했을 때, 서버가 바로 터지지 않게 막고
`500 Internal Server Error`로 응답을 줌

## HttpStatus 상태 코드

### 400 Bad Request
- 상황 : 요청 데이터 자체가 잘못됨 (형식 오류, 유효성 실패 등)
- 예외 : IllegalArgumentException, Bean Validation

### 409 Conflict
- 상황 : 요청은 올바르지만, 현재 상태에서 수행 불가 (논리 위반, 중복 등)
- 예외 : IllegalStateException, 비즈니스 제약

### 401 Unauthorized
- 상황 : 인증되지 않은 사용자
- 예외 : AuthenticationException

### 403 Forbidden
- 상황 : 접근 권한 없음
- 예외 : AccessDeniedException

### 404 Not Found
- 상황 : 요청한 리소스가 없음
- 예외 : EntityNotFoundException

### 500 Internal Server Error
- 상황 : 서버 내부 오류 (예상치 못한 버그)
- 예외 : RuntimeException, NullPointerException
