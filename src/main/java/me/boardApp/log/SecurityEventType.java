package me.boardApp.log;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 로그 문자열이 아니라 의미 있는 이벤트 타입
// 나중에 통계 / 알림 / 감사 로그로 확장 가능
@Getter
@RequiredArgsConstructor
public enum SecurityEventType {

	// INFO
	LOGIN_SUCCESS(SecuritySeverity.INFO, "로그인 성공"),
	LOGOUT_SUCCESS(SecuritySeverity.INFO, "로그아웃 성공"),
	ACCOUNT_UNLOCKED_BY_ADMIN(SecuritySeverity.INFO, "관리자에 의해 유저 ID 잠금 해제"),

	// WARN
	LOGIN_FAIL(SecuritySeverity.WARNING, "로그인 실패"),
	AUTHENTICATION_FAILED(SecuritySeverity.WARNING, "인증 실패"),
	LOGIN_FAIL_UNKNOWN_USER(SecuritySeverity.WARNING, "존재하지 않는 아이디로 로그인 시도"),
	LOCKED_ACCOUNT_LOGIN_ATTEMPT(SecuritySeverity.WARNING, "잠긴 계정 로그인 시도"),
	LOGOUT_FAILED_INVALID_TOKEN(SecuritySeverity.WARNING, "존재하지 않은 토큰으로 로그아웃 시도"),
	ACCESS_DENIED(SecuritySeverity.WARNING, "계정 거부"),
	TOKEN_MISSING(SecuritySeverity.WARNING, "토큰 누락"),
	TOKEN_INVALID(SecuritySeverity.WARNING, "토큰 유효하지 않음"),
	TOKEN_EXPIRED(SecuritySeverity.WARNING, "토큰 만료"),
	TOKEN_UNSUPPORTED(SecuritySeverity.WARNING, "지원하지 않는 토큰"), // 이거는 CRITICAL로 둘 지 고민
	// 일반 사용자 실수 가능성 -> WARNING , 반복/ 패턴 발견시 CRITICAL로 승격. 나중에 통계 기반으로 조정 가능

	// CRITICAL
	LOCKED_ACCOUNT_REFRESH_ATTEMPT(SecuritySeverity.CRITICAL, "잠긴 계정에서 Token Refresh 시도"),
	LOGIN_FAIL_THRESHOLD_EXCEEDED(SecuritySeverity.CRITICAL, "로그인 실패 횟수 허용 초과"),
	REFRESH_REUSED(SecuritySeverity.CRITICAL, "Refresh Token 재사용"),
	REFRESH_LOCKED(SecuritySeverity.CRITICAL, "Refresh Token 잠김"),
	ACCOUNT_LOCKED(SecuritySeverity.CRITICAL, "계정 잠김");

	private final SecuritySeverity severity;
	private final String message;
}
