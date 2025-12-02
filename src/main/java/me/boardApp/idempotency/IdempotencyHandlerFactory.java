//package me.boardApp.idempotency;
//
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@Component
//public class IdempotencyHandlerFactory {
//	// 상태별 전략 매핑 Map
//
//	private final Map<String, IdempotencyHandler> handlers = new HashMap<>();
//
//	public IdempotencyHandlerFactory(
//		CompletedHandler completedHandler,
//		ProcessingHandler processingHandler
//	) {
//		handlers.put("COMPLETED", completedHandler);
//		handlers.put("PROCESSING", processingHandler);
//	}
//
//	public Optional<IdempotencyHandler> getHandler(String status){
//		return Optional.ofNullable(handlers.get(status));
//	}
//}
