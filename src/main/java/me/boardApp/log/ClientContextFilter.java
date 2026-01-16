package me.boardApp.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.boardApp.log.dto.ClientContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// JwtAuthenticationFilter 는 인증 책임
// ClientContextFilter는 요청 환경 수집 책임
// 역할 분리
public class ClientContextFilter extends OncePerRequestFilter {

	public static final String CLIENT_CONTEXT_KEY = "clientContext";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		ClientContext clientContext = ClientContext.from(request);

		// 핵심
		request.setAttribute(CLIENT_CONTEXT_KEY, clientContext);
		// - 이 요청(request) 평생동안
		// 어디서든 꺼내 쓸 수 있다는 뜻

		filterChain.doFilter(request, response);
	}
}
