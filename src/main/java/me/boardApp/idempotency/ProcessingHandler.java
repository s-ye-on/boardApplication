package me.boardApp.idempotency;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class ProcessingHandler implements IdempotencyHandler {
	@Override
	public boolean handle(IdempotencyRecord record, HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_CONFLICT);
		return false;
	}
}
