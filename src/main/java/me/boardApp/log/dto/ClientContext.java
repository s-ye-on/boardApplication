package me.boardApp.log.dto;

import jakarta.servlet.http.HttpServletRequest;

public record ClientContext(
	String ipAddress,
	String userAgent, // 사용한 기기나, 웹 브라우저
	String requestUri,
	String httpMethod  // request.getMethod() -> HTTP 프로토콜 레벨의 메서드 (GET, POST, PUT ...)
) {

	// static factory method
	// HTTP 파싱 책임을 DTO 내부에 숨김
	// AuthService는 파싱 방법을 몰라도 됨
	public static ClientContext from(HttpServletRequest request) {
		return new ClientContext(
			extractIp(request),
			request.getHeader("User-Agent"),
			request.getRequestURI(),
			request.getMethod()
		);
	}

	// null-safe 처리
	// 테스트나 서비스를 바로 부를 때 사용
	public static ClientContext system() {
		return new ClientContext("SYSTEM", "SYSTEM", "-", "-");
	}

	// 요청 파싱 책임이 dto에 있음
	private static String extractIp(HttpServletRequest request) {
		// 프록시/로드밸런서 환경 고려 (?)
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
