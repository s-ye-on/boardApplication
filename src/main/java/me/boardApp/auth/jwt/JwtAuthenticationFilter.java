package me.boardApp.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	// 역할 :
	// 매 요청 마다 Authorization: Bearer <token> 헤더를 읽고
	// 토큰 유효성 검사 -> OK면 SecurityContext에 인증 정보 세팅

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
																	HttpServletResponse response,
																	FilterChain filterChain)
		throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");

		if(authHeader != null && authHeader.startsWith("Bearer ")){
			String token = authHeader.substring(7);

			if(jwtTokenProvider.validateToken(token)){
				Long userId = jwtTokenProvider.getUserId(token);

				User user = userRepository.findById(userId).orElse(null);
				if(user != null){
					CustomUserDetails principal = new CustomUserDetails(user);
					var auth = new UsernamePasswordAuthenticationToken(
						principal,
						null,
						principal.getAuthorities()
					);
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			}
		}
		filterChain.doFilter(request, response);
	}
}
