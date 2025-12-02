//package me.boardApp.idempotency;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//@Component
//public class IdempotencyFilter extends OncePerRequestFilter {
//	// 멱등성 처리필터 or AOP
//
//	private final IdempotencyRecordRepository repository;
//	private final IdempotencyHandlerFactory factory;
//
//	public IdempotencyFilter(IdempotencyRecordRepository repository,
//													 IdempotencyHandlerFactory factory) {
//		this.repository = repository;
//		this.factory = factory;
//	}
//
//	@Override
//	protected void doFilterInternal(HttpServletRequest request,
//																	HttpServletResponse response,
//																	FilterChain filterChain) throws ServletException, IOException {
//
//		String key = request.getHeader("Idempotency-Key");
//
//		if(key == null){
//			filterChain.doFilter(request, response);
//			return;
//		}
//		Optional<IdempotencyRecord> recordOpt = repository.findById(key);
//
//		if(recordOpt.isPresent()){
//			IdempotencyRecord record = recordOpt.get();
//
//			// 상태에 맞는 핸들러 호출
//			Optional<IdempotencyHandler> handlerOpt = factory.getHandler(record.getStatus());
//
//			if(handlerOpt.isPresent()){
//				boolean continueFilter = handlerOpt.get().handle(record, response);
//				if(!continueFilter) return;
//			}
//		}
//		else{
//			//최초 요청 -> PROCESSING 저장
//			IdempotencyRecord record = new IdempotencyRecord();
//			record.setRequestId(key);
//			record.setStatus("PROCESSING");
//			record.setCreatedAt(LocalDateTime.now());
//			repository.save(record);
//		}
//
//		// 이후 필터 -> 컨트롤러 -> 서비스 실행
//		// 정상 응답 나오면 기록 업데이트 필요함
//
//		IdempotencyResponseWrapper wrapper = new IdempotencyResponseWrapper(response);
//		filterChain.doFilter(request, wrapper);
//
//		// 응답 저장
//		IdempotencyRecord completed = repository.findById(key).get();
//		completed.setStatus("COMPLETED");
//		completed.setStatusCode(response.getStatus());
//		completed.setResponseBody(wrapper.getResponseBody());
//		repository.save(completed);
//	}
//}
