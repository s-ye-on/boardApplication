package me.boardApp.global.exception;

public class BoardException extends ApiException {

	public BoardException(ExceptionCode code) {
		super(code);
	}

	public BoardException(ExceptionCode code, String message) {
		super(code, message);
	}
}
