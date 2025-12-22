package me.boardApp.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.boardApp.log.dto.ClientContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {
	// 단순 서비스 로직 x
	// 보안 이벤트를 기록하는 책임
	// 이벤트를 처리하는 역할 (로그, 저장, 알림 포함 가능)

	private final SecurityEventRepository repository;

	public void record(
		SecurityEventType type,
		Long userId,
		ClientContext context,
		String message
	) {
		if (context == null) {
			context = ClientContext.system(); // fallback
		}

		SecurityEvent event = SecurityEvent.of(
			type,
			userId,
			context,
			message
		);

		repository.save(event);

		log.info("[SECURITY] {} - userId={}", type, userId);
	}
}

