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
		// HTTP Authorization 헤더 읽기 (예상 : "Bearer <토큰>")
		String authHeader = request.getHeader("Authorization");

		// 헤더가 존재하고 "Bearer "로 시작하면 토큰을 꺼낸다
		if(authHeader != null && authHeader.startsWith("Bearer ")){
			String token = authHeader.substring(7); // 실제 토큰 문자열(앞의 "Bearer " 제거)

			// 토큰 검증(서명/만료 등)
			if(jwtTokenProvider.validateToken(token)){
				Long userId = jwtTokenProvider.getUserId(token);

				User user = userRepository.findById(userId).orElse(null);
				if(user != null){
					// User 엔티티 -> Spring Security UserDetails로 변환
					CustomUserDetails principal = new CustomUserDetails(user);

					//Authentication 객체 생성
					// credentials에 null 넣는건 일반적(토큰 기반이기 때문에 비밀번호 사용 x)
					var auth = new UsernamePasswordAuthenticationToken(
						principal,
						null,
						principal.getAuthorities()
					);
					// SecurityContext에 인증 정보 저장 -> 이후 Controller에서 @AuthenticationPrincipal 등으로 사용 가능
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
				// user == null이면 인증 안하고 그냥 넘어간다(익명 요청)
			}
			// 토큰이 검증 실패면 그냥 인증 정보 없이 이어진다 (필터 체인 계속)
		}
		// 다음 필터(또는 최종 리소스)로 진행
		filterChain.doFilter(request, response);
	}
}
