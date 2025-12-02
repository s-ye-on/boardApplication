package me.boardApp.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@WebFilter(urlPatterns = "/*")
@Slf4j
public class LogFilter implements Filter {

	@Override
	public void doFilter(ServletRequest servletRequest,
											 ServletResponse servletResponse,
											 FilterChain filterChain)throws IOException, ServletException{
		// 요청이 들어올 때 해야할 작업
		long start = System.currentTimeMillis();
		log.info("LogFilter.doFilter() begins for {}",
			((HttpServletRequest)servletRequest).getRequestURI());

		// 다음 필터 처리
		filterChain.doFilter(servletRequest, servletResponse);

		// 요청을 처리하고 나갈 때 해야 할 작업
		long timeElapsed = System.currentTimeMillis() - start;
		log.info("LogFilter.doFilter() returns for {} status {} in {} ms",
			((HttpServletRequest)servletRequest).getRequestURI(),
			((HttpServletResponse)servletResponse).getStatus(),
			timeElapsed);
	}
}
