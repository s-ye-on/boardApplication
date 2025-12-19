# Spring Boot 전역 예외 처리 설계하기 - GlobalExceptionHandler + ApiException

## 전역 예외 처리기 (Global Exception Handler)
스프링의 `@RestControllerAdvice`는 모든 컨트롤러에서 발생한 예외를 감지해서 각 예외 타입에 맞는 응답(ResponseEntity)을 만들어주는 역할을 함
-> "어디서든 예외가 발생해도, 컨트롤러마다 try-catch 쓰지 말고 한 곳에서 처리하자!"

> 이 글에서는 REST API 응답(JSON)에 집중하기 위해  
> 전역 예외 처리기 클래스명을 `ApiExceptionHandler`로 사용했다.  
> 역할 자체는 GlobalExceptionHandler와 동일하다.

### 전역 예외 처리기의 핵심 역할 3가지

| 역할          | 설명                                             | 예시                                         |
|-------------|------------------------------------------------|--------------------------------------------|
| 예외 감지 및 처리  | `@ExceptionHandler`로 특정 예외가 발생했을 때 실행할 메서드를 지정 | @ExceptionHandler(ApiException.class)      |
| 공통 응답 형식 제공 | 예외마다 다른 응답을 주는게 아니라, 일관된 JSON 포맷으로 변환          | {"status" : 400, "message" : "잘못된 요청입니다."} |
| 로깅 및 모니터링   | Logger로 예외를 파일에 기록해서 추적 가능                     | log.error("회원 조회 실패", e);                  |

## ApiException / ExceptionCode의 역할 정의 
- ApiException은 모든 커스텀 예외의 부모 클래스 역할
- RuntimeException을 상속한 언체크 예외
- ExceptionCode (enum)를 통해 HTTP 상태 코드와 메시지를 함께 관리 

### 설계 포인트 : 예외를 객체로 관리하기 
- 모든 비즈니스 예외는 `ApiException` 을 상속한다
- HTTP 상태 코드와 메시지는 `ExceptionCode` enum에서 중앙 관리한다
- 서비스 계층에서는 상태 코드나 ResponseEntity를 몰라도 된다 
    -> 예외만 던지고, 응답은 전역 예외 처리기에 맡긴다

> 서비스 계층이 HTTP 개념(ResponseEntity, Status Code)을 알게 되면  
> 도메인 로직과 웹 계층이 강하게 결합된다.  
> 예외를 던지고 응답은 전역 예외 처리기에 맡기는 구조가  
> 테스트와 유지보수에 더 유리하다.

### 전역 예외 처리기에 담기는 코드 종류
| 구분                   | 역할                                                      | 예시                                                       |
|----------------------|---------------------------------------------------------|----------------------------------------------------------|
| 커스텀 예외 처리            | 개발자가 직접 던진 ApiException 처리                              | @ExceptionHandler(ApiException.class)                    |
| Validation 예외 처리     | @Valid, @NotBlank 등 유효성 검증 실패 처리                        | @ExceptionHandler(MethodArgumentNotValidException.class) |
| DB나 Null 등 시스템 예외 처리 | NullPointerException, DataIntegrityViolationException 등 | @ExceptionHandler(RuntimeException.class)                |
| 로깅 및 응답 포맷 통일        | ResponseEntity로 JSON 응답 통일                              | ResponseEntity.status(...).body(...)                     |


### 완성형 예시 코드 구조
ExceptionCode.java
```java
package me.boardApp.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {

    // 400 BAD REQUEST
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
  
    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");

    private final HttpStatus status;
    private final String message;
}
```
ApiException.java
```java
package me.boardApp.global.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ExceptionCode exceptionCode;

    public ApiException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage()); // 예외 메시지 설정
        this.exceptionCode = exceptionCode;
    }
}
```
- 즉 서비스에서 이렇게 던질 수 있음
- `throw new ApiException(ExceptionCode.USER_NOT_FOUND);`

ApiExceptionHandler.java (전역 예외 처리기)
```java
package me.boardApp.global.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // ✅ 커스텀 예외 처리
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException e) {
        var code = e.getExceptionCode();
        log.warn("Custom Exception 발생: {}", code.getMessage());
        return ResponseEntity
            .status(code.getStatus())
            .body(new ErrorResponse(code.getStatus().value(), code.getMessage()));
    }

    // ✅ Validation 실패 처리
	// @Valid, @NotBlank 등 Bean Validation 실패 시 발생하는 예외를 한 번에 처리한다 
	// 필드별 상세 메시지를 내려주고 싶다면 ErrorResponse 구조를 확장하면 된다
	@ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<?> handleValidationException(Exception e) {
        log.warn("Validation 실패: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "입력값이 올바르지 않습니다."));
    }			
  
    // ✅ 예상치 못한 예외 처리 (마지막 방어선)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleUnexpectedException(RuntimeException e) {
        log.error("Unexpected Exception", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "서버 내부 오류가 발생했습니다."));
    }

    // ✅ 응답용 DTO (일관된 응답 포맷)
    // ErrorResponse는 Java record를 사용해 불변 객체로 정의했다
    // 예외 응답은 상태 변경이 필요 없고, 간결함이 중요하다고 생각했다
    record ErrorResponse(int status, String message) {}
}
```
- 예상치 못한 예외는 반드시 stack trace를 로그로 남긴다
- 클라이언트에는 내부 구조를 노출하지 않는다 

### 예외 처리 전체 흐름
1. 서비스 / 도메인 로직에서 `ApiException` 발생
2. 예외가 컨트롤러 밖으로 전파됨 
3. `@RestControllerAdvice` 가 예외를 가로챔 
4. 가장 적합한 `@ExceptionHandler` 메서드 실행
5. HTTP 상태 코드 + 메시지를 포함한 JSON 응답 반환 

```json
{
  "status": 404,
  "message": "존재하지 않는 사용자입니다."
}
```

### 이름별 비교 요약표

| 이름                     | 일반적인 용도                       | 예시                           |
|------------------------|-------------------------------|------------------------------|
| GlobalExceptionHandler | 전체 애플리케이션 공통 예외 처리            | REST + MVC 통합 예외 처리          |
| ApiExceptionHandler    | REST API 전용 예외 처리 (JSON 반환)   | @RestControllerAdvice와 함께 사용 |
| WebExceptionHandler    | 웹페이지(View) 전용 예외 처리 (HTML 반환) | @ControllerAdvice와 함께 사용     |

## 📚마무리 정리 
- 전역 예외 처리는 @RestControllerAdvice를 통해 한 곳에서 관리할 수 있다
- 서비스 계층은 예외만 던지고, HTTP 응답 책임은 전역 예외 처리기에 맡긴다 
- ApiException + ExceptionCode 구조를 사용하면 
  - 상태 코드와 메시지를 중앙에서 관리할 수 있고
  - 비즈니스 로직이 HTTP 개념에 의존하지 않게 된다
- Validation, 비즈니스 예외, 예상치 못한 예외를 계층별로 분리해 처리하는 것이 중요하다 
- 예상치 못한 예외는 반드시 로그를 남기고, 클라이언트에는 내부 정보를 노출하지 않는다  
  
전역 예외 처리 설계는  
**에러를 어떻게 "숨길지"가 아니라,  
어떻게 "통제할지"를 결정하는 문제다**