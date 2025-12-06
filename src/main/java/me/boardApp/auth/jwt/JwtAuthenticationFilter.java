package me.boardApp.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.exception.AuthorizationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
		// HTTP Authorization 헤더 읽기 (예상 : "Bearer <토큰>")
		String authHeader = request.getHeader("Authorization");

		// Authorization 헤더가 없으면 이 필터는 건너 뛰고, 나머지 체인에 맡긴다
		// 토큰 없는 요청은 익명 요청으로 두고, 나중에 시큐리티가 401/403 판단
		if(authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String token = authHeader.substring(7);

			// 유효성 검사 (문제 있으면 AuthorizationException 발생)
			// (서명/만료 등)
			jwtTokenProvider.validateToken(token);

			Long userId = jwtTokenProvider.getUserId(token);
			User user = userRepository.findById(userId).orElse(null);

			if(user != null){
				// User 엔티티 -> Spring Security UserDetails로 변환
				CustomUserDetails principal = new CustomUserDetails(user);

				// Authentication 객체 생성
				Authentication auth = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					principal.getAuthorities()
				);
				// SecurityContext에 인증 정보 저장 -> 이후 Controller에서 @AuthenticationPrincipal 등으로 사용 가능
				SecurityContextHolder.getContext().setAuthentication(auth);
			}

			filterChain.doFilter(request, response);

		} catch (AuthorizationException ex) {
			// 여기서 직접 응답을 써도 되고, request attribute에 태워서 EntryPoint로 넘겨도 됨
			// 보통은 EntryPoint에서 공통 처리하는게 깔끔하니 attribute에 태우자
			request.setAttribute("authError", ex);
			// 나머지는 SecurityException 흐름을 넘기기 위해 filterChain을 계속 타게 두지 말고 종료
			// 대신 EntryPoint가 호출되도록 doFilter를 호출하지 않고 바로 처리하고 return 하는 방법도 있음
			// 여기서 sendError 하지말고, 시큐리티가 EntryPoint를 호출하게 맡기는 쪽이 좋음
			throw ex; // ApiException / AuthorizationException 전역 핸들러 또는 EntryPoint로 전달
		}
	}
}
