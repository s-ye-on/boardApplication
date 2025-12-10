package me.boardApp.config;

import me.boardApp.auth.jwt.JwtAccessDeniedHandler;
import me.boardApp.auth.jwt.JwtAuthenticationEntryPoint;
import me.boardApp.auth.jwt.JwtAuthenticationFilter;
import me.boardApp.auth.jwt.JwtTokenProvider;
import me.boardApp.domain.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 환경설정(Bean 등록)을 담당하는 클래스 이름을 ~config라고 지음
// @Configuration -> 스프링 설정 클래스임을 알려주는 어노테이션
@Configuration
@EnableMethodSecurity // 메서드 수준 보안 활성화

public class SecurityConfig {
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
																												 UserRepository userRepository) {
		return new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
	}

	// 1. 어떤 URL에 보안 걸지 & 로그인 방식 정의
	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		JwtAuthenticationEntryPoint authenticationEntryPoint,
		JwtAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/",
					"/login",      // 로그인 화면
					"/users/join",    //JSON 회원 가입
					"/users/join-form",  // 폼 회원 가입
					"/auth/login",    // JWT 로그인 API
					"/boards.html",
					"/h2-console/**",
					"/css/**",
					"/js/**",
					"/images/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/* 추가로 조금 더 알면 좋을점들
	/login은 formLogin() 쓰는 순간 자동으로 permitAll 처리돼서
굳이 requestMatchers 에 /login을 넣을 필요는 없다 (넣어도 문제는 없음)

h2 콘솔을 실제로 브라우저에서 쓰고 싶다면, 나중에 이런 설정을 추가로 넣을 수도 있음
   http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
(안그럼 프레임 관련 보안 때문에 콘솔이 안뜨는 경우가 있음)

	 */

	// 2. 메모리에 테스트 계정 하나 생성
//	@Bean
//	public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
//		UserDetails user = User.builder()
//			.username("testUser")
//			.password(passwordEncoder.encode("1234"))
//			.roles("USER")
//			.build();
//
//		return new InMemoryUserDetailsManager(user);
//	}
	// Spring Security 사용해서 내가 만든 user 엔티티를 사용할거기에 이제 안녕~
}
