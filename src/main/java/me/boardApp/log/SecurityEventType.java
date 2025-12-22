package me.boardApp.log;

// 로그 문자열이 아니라 의미 있는 이벤트 타입
// 나중에 통계 / 알림 / 감사 로그로 확장 가능
public enum SecurityEventType {
	LOGIN_SUCCESS,
	LOGIN_FAIL,
	LOGOUT,
	REFRESH_ISSUED,
	REFRESH_REUSED,
	REFRESH_LOCKED,
	ACCOUNT_LOCKED,
	ACCESS_DENIED
}
