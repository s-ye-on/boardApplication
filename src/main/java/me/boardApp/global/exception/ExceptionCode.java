package me.boardApp.global.exception;

import lombok.RequiredArgsConstructor;
import lombok.Getter;
import me.boardApp.log.SecurityEventType;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {
	/**
	 * SERVER ERROR
	 */
	SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다", null),


	// 400
	// 입력값이 유효하지 않음(요청 데이터 문제)
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다", null),
	INVALID_REAL_NAME(HttpStatus.BAD_REQUEST, "실명이 일치하지 않습니다", null),
	INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임이 일치하지 않습니다", null),
	SAME_NICKNAME(HttpStatus.BAD_REQUEST, "현재 닉네임과 동일합니다", null),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다", null),
	INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일이 일치하지 않습니다", null),
	USER_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "사용자 정보가 올바르지 않습니다", null),
	NOT_LOCKED_ACCOUNT(HttpStatus.BAD_REQUEST, "잠겨 있지 않은 계정입니다", null),
	INACTIVATION_ACCOUNT(HttpStatus.BAD_REQUEST, "탈퇴한 계정입니다", null),

	// 401 Unauthorized 인증 필요(로그인 안됨) or 토큰 관련
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", null), // 인증 실패의 포괄 개념(fallback으로 처리)
	TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "인증 토큰을 없습니다", SecurityEventType.TOKEN_MISSING),
	TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다", SecurityEventType.TOKEN_INVALID),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다", SecurityEventType.TOKEN_EXPIRED),
	TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "지원되지 않는 인증 토큰입니다", SecurityEventType.TOKEN_UNSUPPORTED),
	REFRESH_REUSED(HttpStatus.UNAUTHORIZED, "재사용된 리프레시 토큰입니다", SecurityEventType.REFRESH_REUSED),

	//403 FORBIDDEN
	// "인증은 되었지만, 권한이 없는 경우"
	FORBIDDEN_ADMIN(HttpStatus.FORBIDDEN, "관리자 권한이 없습니다", null),
	FORBIDDEN_POST_UPDATE(HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다", null),
	FORBIDDEN_POST_DELETE(HttpStatus.FORBIDDEN, "게시글 삭제 권한이 없습니다", null),
	FORBIDDEN_POST_ACCESS(HttpStatus.FORBIDDEN, "게시글에 대한 권한이 없습니다", null),

	// 404
	NOT_FOUND_BOARD(HttpStatus.NOT_FOUND, "게시판이 존재하지 않습니다", null),
	NOT_FOUND_TEMP_BOARD(HttpStatus.NOT_FOUND, "임시 게시판이 존재하지 않습니다", null),
	NOT_FOUND_COMMENT(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다", null),
	NOT_FOUND_POST(HttpStatus.NOT_FOUND, "게시글이 존재하지 않습니다", null),
	NOT_FOUND_NICKNAME(HttpStatus.NOT_FOUND, "존재하지 않는 닉네임 입니다", null),
	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "가입하신 아이디가 존재하지 않습니다", null),
	NOT_FOUND_NOTICE_POST(HttpStatus.NOT_FOUND, "공지글이 존재하지 않습니다", null),

	// 409
	// IllegalStateException -> HttpStatus.CONFLICT (409)
	// 요청은 유효하지만, 현재 상태에서 수행할 수 없음
	DUPLICATE_TEMP_BOARD(HttpStatus.CONFLICT, "임시 게시판은 하나만 존재할 수 있습니다", null),
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 존재하는 닉네임 입니다", null),
	CANNOT_DELETE_TEMP_BOARD(HttpStatus.CONFLICT, "임시 게시판은 삭제할 수 없습니다.", null),
	CANNOT_UPDATE_TEMP_BOARD(HttpStatus.CONFLICT, "임시 게시판은 수정할 수 없습니다.", null),
	CANNOT_COMMENT_NOTICE_POST(HttpStatus.CONFLICT, "공지글에는 댓글을 작성할 수 없습니다", null),


	// 423
	// LOCKED
	// 계정 자체가 잠긴 상태
	LOCKED_ACCOUNT(HttpStatus.LOCKED, "계정이 잠겨 있습니다. 본인 인증이 필요합니다", null),
	;

	private final HttpStatus status;
	private final String message;
	// 선택적 매핑
	// 모든 예외가 보안 이벤트일 필요 없음
	// 의미 있는 것만 감사
	// 나머지는 fallback
	private final SecurityEventType securityEventType;

	// 기본값으로 떨어뜨리는 헬퍼
	// 원인을 가진 쪽에서 의미를 결정하기로 함
	// ExceptionCode는 무슨일이 일어났는지를 알고 있고,
	// SecurityEventType은 "그걸 어떻게 기록할지"만 알아야한다
	// 원인이 해석을 부르는 구조가 더 자연스럽다
	// 주의 : 이 메서드는 JwtAuthenticationEntryPoint에서만 사용해야 한다
	public SecurityEventType toSecurityEventTypeOr(SecurityEventType fallback) {
		return securityEventType != null ? securityEventType : fallback;
	}
}
