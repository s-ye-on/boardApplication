package me.boardApp.global.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
// 전역 예외 처리기
// 이 클래스는 모든 컨트롤러에서 던져지는 예외를 가로채서 대신 처리해줌
public class ApiExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	// 내부에 있는 메서드들은 특정 예외 타입이 발생했을 때만 실행됨
	// 이건 우리가 정의한 커스텀 예외를 처리하는 메서드
	/*
	동작 순서
	1. ApiException이 던져짐
	2. 스프링이 이 예외는 handleApiException()에서 처리하네 하고 이 메서드 호출
	3. ExceptionCode에서 HTTP 상태코드랑 메시지를 꺼냄
	4. ResponseEntity로 예브게 HTTP 응답 만들어 반환
	 */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<?> handleApiException(ApiException e) {
		var code = e.getExceptionCode();

		log.warn("Custom Exception 발생: {}", code.getMessage());

		return ResponseEntity
			.status(code.getStatus())
			.body(e.getMessage());
	}

	// ConstraintViolationException : @RequestParam, @PathVariable, @ModelAttribute 등에 @Valid나
	// 제약조건(@NotNull, @Min 등)을 걸었을 때 위반되면 터짐
	// @Valid @RequestParam 등 단일 파라미터 검증 실패

	// MethodArgumentNotValidException : @RequestBody로 JSON을 받을 때, DTO에 설정된 제약 조건(@NotBlank, @Email)을
	// 위반하면 이 예외가 터짐
	// 내가 만든 커스텀 어노테이션이 Constraint로 등록되어 있다면 기본적으로 MethodArgumentNotValidException이나 ConstraintViolationException 중 하나로 처리
	// @Valid, @RequestBody DTO 검증 실패

	// 유효성 검증(Validation)에 실패했을 때 처리하는 메서드
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
	public String handleBeanValidationFailed(Exception e) {
		return e.getMessage();
	}

	// 예상하지 못한 모든 런타임 예외를 처리하는 마지막 안전장치
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(RuntimeException.class)
	public String handleUncaughtException(RuntimeException ex) {
		return ex.getMessage();
	}
}
