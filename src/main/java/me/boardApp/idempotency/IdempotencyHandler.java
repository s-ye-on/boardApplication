package me.boardApp.idempotency;

import jakarta.servlet.http.HttpServletResponse;

// 전략 패턴 기반 핸들러 맵
public interface IdempotencyHandler {
	boolean handle(IdempotencyRecord record, HttpServletResponse response);
}
