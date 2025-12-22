package me.boardApp.log;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.boardApp.log.dto.ClientContext;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private SecurityEventType type;

	private Long userId; // null 가능 (인증 전 실패)
	// userId가 null일 수 있는 이유
	// 로그인 실패 , 토큰 위조, 인증 이전 단계

	// httpMethod는 Controller메서드 X
	// HTTP 요청 메서드 O (GET/POST/...)
	private String httpMethod;
	private String ipAddress;
	private String userAgent;
	// requestURI : 클라이언트가 요청한 URL 경로 문자열이 담김
	// ex) /auth/login
	private String requestURI;

	private LocalDateTime createdAt;

	private String message;

	public static SecurityEvent of(
		SecurityEventType type,
		Long userId,
		ClientContext clientContext,
		String message
	) {
		SecurityEvent event = new SecurityEvent();
		event.type = type;
		event.userId = userId;
		event.ipAddress = clientContext.ipAddress();
		event.userAgent = clientContext.userAgent();
		event.requestURI = clientContext.requestUri();
		event.httpMethod = clientContext.httpMethod();
		event.createdAt = LocalDateTime.now();
		event.message = message;

		return event;
	}
}
