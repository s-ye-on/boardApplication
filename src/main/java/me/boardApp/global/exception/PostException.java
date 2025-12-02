package me.boardApp.global.exception;

public class PostException extends ApiException {

	public PostException(ExceptionCode code) {
		super(code);
	}

	public PostException(ExceptionCode code, String message) {
		super(code, message);
	}
}
