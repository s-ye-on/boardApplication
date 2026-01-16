package me.boardApp.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.log.ClientContextFilter;
import me.boardApp.log.SecurityEventService;
import me.boardApp.log.SecurityEventType;
import me.boardApp.log.dto.ClientContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 권한은 있지만 Role 부족 -> 여기서 403 응답
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityEventService securityEventService;

	@Override
	public void handle(HttpServletRequest request,
										 HttpServletResponse response,
										 AccessDeniedException accessDeniedException
	) throws IOException {

		Long userId = null;

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		// auth != null - 필터 체인 중 예외가 빨리 터진 경우. - SecurityContext가 아직 채워지지 않았을 수도 있음
		// auth.isAuthenticated() - 토큰은 있지만 인증 객체가 완성되지 않은 상태일 수 있음
		// !(auth instanceof AnonymousAuthenticationToken) - permitAll URL. - 익명 사용자. - 스프링이 자동으로 넣은 가짜 Authentication
		// 이걸 안걸러내면 :
		// - 익명 요청도 "userId 있는 것처럼" 감사 로그에 남음
		// - 보안 감사 데이터 오염

		if(auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {

			Object principal = auth.getPrincipal();
			// 왜 principal을 다시 캐스팅하나?
			// principal의 실제 타입은 상황마다 다름
			// 상황 : JWT 인증 성공 principal : CustomUserDetails
			// 상황 : 익명 principal : "anonymousUser" (String)
			// 상황 : 다른 인증 방식 principal : 다른 타입
			// 그래서 무조건 캐스팅 X -> instanceof 체크 필수

			if(principal instanceof CustomUserDetails customUserDetails) {
				userId = customUserDetails.getId();
			}
		}

		ClientContext context = (ClientContext) request.getAttribute(ClientContextFilter.CLIENT_CONTEXT_KEY);

		securityEventService.record(
			SecurityEventType.ACCESS_DENIED,
			userId,
			context
		);

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("""
			{"code":"FORBIDDEN","message":"이 리소스에 접근할 권한이 없습니다."}
			""");
	}
}
