package me.boardApp.global.exception;

public class AuthorizationException extends ApiException {

	public AuthorizationException(ExceptionCode code) {
		super(code);
	}

	public AuthorizationException(ExceptionCode code, String message) {
		super(code, message);
	}
}
