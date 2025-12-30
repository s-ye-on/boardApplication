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

		securityEventService.record(
			SecurityEventType.AUTHENTICATION_FAILED,
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
