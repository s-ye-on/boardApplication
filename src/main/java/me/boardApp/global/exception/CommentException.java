package me.boardApp.global.exception;

public class CommentException extends ApiException {
	public CommentException(ExceptionCode code) {
		super(code);
	}

	public CommentException(ExceptionCode code, String message) {
		super(code, message);
	}
}
