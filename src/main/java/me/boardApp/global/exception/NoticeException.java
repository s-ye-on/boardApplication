package me.boardApp.global.exception;

public class NoticeException extends ApiException {
	public NoticeException(ExceptionCode code) {
		super(code);
	}

	public NoticeException(ExceptionCode code, String message) {
		super(code, message);
	}
}
