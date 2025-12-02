//package me.boardApp.idempotency;
//
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//
//@Component
//public class CompletedHandler implements IdempotencyHandler {
//
//	@Override
//	public boolean handle(IdempotencyRecord record, HttpServletResponse response) {
//		response.setStatus(record.getStatusCode());
//		response.getWriter().write(record.getResponseBody() != null ? record.getResponseBody() : "");
//		return false; // 기존 로직 재실행 금지
//	}
//}
