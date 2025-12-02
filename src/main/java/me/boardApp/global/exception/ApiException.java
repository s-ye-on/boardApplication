package me.boardApp.global.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
	private final ExceptionCode exceptionCode;

	// 생성자 오버로딩
	public ApiException(ExceptionCode exceptionCode, String message) {
		// 예외 코드와 메시지를 둘 다 명시적으로 지정할 때 쓰는 생성자
		super(message); // 예외 메시지 설정
		this.exceptionCode = exceptionCode;
	}

	public ApiException(ExceptionCode code) {
		// 여기서 this는 첫번째 생성자를 호출 의미
		// 예외 코드만 넘기고, 기본 메시지를 사용하고 싶을 때 사용
		this(code, code.getMessage());
	}
}
