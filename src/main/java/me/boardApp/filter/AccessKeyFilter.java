//package me.boardApp.filter;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.annotation.WebFilter;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
///// todo :
///// 필터는 좀 더 공부해서 다시 만들어보자
///// 지금은 그냥 따라 치기만 해놨음
//
//@WebFilter(urlPatterns = "/*")
//@Slf4j
//// @Slf4j : private static final Logger log = ... 같은 필드가 자동 생성 된다
//public class AccessKeyFilter extends OncePerRequestFilter {
//	@Override
//	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//		throws ServletException, IOException {
//		log.info("AccessKeyFilter.doFilterInternal() begins");
//		String authorization = request.getHeader("Authorization");
//
//		if(authorization != null && authorization.startsWith("Bearer ")) {
//			String token = authorization.replace("Bearer ", "").trim();
//
//			if("csy-access-key".equals(token)) {
//				filterChain.doFilter(request, response);
//				log.info("AccessKeyFilter.doFilterInternal() returns");
//				return;
//			}
//		}
//		response.setStatus(HttpStatus.UNAUTHORIZED.value());
//		log.info("AccessKeyFilter.doFilterInternal() returns Unauthorized");
//	}
//}
