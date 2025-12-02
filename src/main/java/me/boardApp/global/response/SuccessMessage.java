package me.boardApp.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessMessage {
	// 원래 클래스 네이밍이 ResponseMessage였지만 너무 포괄적인 의미임
	// ResponseMessage라면 ExceptionMessage도 여기 담겨야함
	LOGIN_SUCCESS(HttpStatus.OK, "로그인 성공"),
	LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃 완료");

	private final HttpStatus status;
	private final String message;

}
