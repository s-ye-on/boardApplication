package me.boardApp.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.boardApp.global.exception.AuthorizationException;
import me.boardApp.global.exception.ExceptionCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

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
