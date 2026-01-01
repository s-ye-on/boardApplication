package me.boardApp.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.boardApp.global.exception.AuthorizationException;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.log.ClientContextFilter;
import me.boardApp.log.SecurityEventService;
import me.boardApp.log.SecurityEventType;
import me.boardApp.log.dto.ClientContext;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증 실패 응답 처리 401
@Component // Spring Security가 인증 실패할 때 자동으로 호출되는 컴포넌트
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityEventService securityEventService;

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		Object authError = request.getAttribute("authError");
		ExceptionCode exceptionCode = (authError instanceof AuthorizationException authorizationException)
			? authorizationException.getExceptionCode()
			: ExceptionCode.UNAUTHORIZED;

		// 인증 실패는 보안 사건
		// Controller 단까지 도달하지 않음
		// 로그 위치가 정책 경계와 일치
		ClientContext context = (ClientContext) request.getAttribute(ClientContextFilter.CLIENT_CONTEXT_KEY);

		// exceptionCode가 이벤트 타입을 알고 있게 했기에 가능함
		// 토큰이 어떤 문제인지에 따라 감사 로그를 정확히 적고 싶기에 이렇게 만들었음
		// switch case 분기 없기에 좀 더 깔끔함
		// 원인 중심 설계 -> "원인이 의미를 결정한다"
		SecurityEventType eventType = exceptionCode.toSecurityEventTypeOr(SecurityEventType.AUTHENTICATION_FAILED);

		securityEventService.record(
			eventType,
			null,
			context
		);

		response.setStatus(exceptionCode.getStatus().value());
		response.setContentType("application/json;charset=UTF-8");

		String body = """
			{
			  "code": "%s",
			  "message": "%s"
			}
			""".formatted(exceptionCode.name(), exceptionCode.getMessage());

		response.getWriter().write(body);
	}
}
