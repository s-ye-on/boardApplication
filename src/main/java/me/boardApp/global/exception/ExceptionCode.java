package me.boardApp.global.exception;

import lombok.RequiredArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {
	/**
	 * SERVER ERROR
	 */
	SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다"),


	// 400
	// 입력값이 유효하지 않음(요청 데이터 문제)
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
	INVALID_REAL_NAME(HttpStatus.BAD_REQUEST, "실명이 일치하지 않습니다"),
	INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임이 일치하지 않습니다"),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
	INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일이 일치하지 않습니다"),
	USER_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "사용자 정보가 올바르지 않습니다"),

	// 401 Unauthorized 인증 필요(로그인 안됨)


	//403 FORBIDDEN
	// "인증은 되었지만, 권한이 없는 경우"
	FORBIDDEN_ADMIN(HttpStatus.FORBIDDEN, "관리자 권한이 없습니다"),
	FORBIDDEN_POST_UPDATE(HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다"),
	FORBIDDEN_POST_DELETE(HttpStatus.FORBIDDEN, "게시글 삭제 권한이 없습니다"),
	FORBIDDEN_POST_ACCESS(HttpStatus.FORBIDDEN, "게시글에 대한 권한이 없습니다"),

	// 404
	NOT_FOUND_BOARD(HttpStatus.NOT_FOUND, "게시판이 존재하지 않습니다"),
	NOT_FOUND_TEMP_BOARD(HttpStatus.NOT_FOUND, "임시 게시판이 존재하지 않습니다"),
	NOT_FOUND_COMMENT(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다"),
	NOT_FOUND_POST(HttpStatus.NOT_FOUND,"게시글이 존재하지 않습니다"),
	NOT_FOUND_NICKNAME(HttpStatus.NOT_FOUND, "존재하지 않는 닉네임 입니다"),
	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "가입하신 아이디가 존재하지 않습니다"),
	NOT_FOUND_NOTICE_POST(HttpStatus.NOT_FOUND, "공지글이 존재하지 않습니다"),

	// 409
	// IllegalStateException -> HttpStatus.CONFLICT (409)
	// 요청은 유효하지만, 현재 상태에서 수행할 수 없음
	DUPLICATE_TEMP_BOARD(HttpStatus.CONFLICT, "임시 게시판은 하나만 존재할 수 있습니다"),
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 존재하는 닉네임 입니다"),
	CANNOT_DELETE_TEMP_BOARD(HttpStatus.CONFLICT, "임시 게시판은 삭제할 수 없습니다."),
	CANNOT_UPDATE_TEMP_BOARD(HttpStatus.CONFLICT, "임시 게시판은 수정할 수 없습니다."),
	CANNOT_COMMENT_NOTICE_POST(HttpStatus.CONFLICT, "공지글에는 댓글을 작성할 수 없습니다"),

	;

	private final HttpStatus status;
	private final String message;
}
