package me.boardApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// 환경설정(Bean 등록)을 담당하는 클래스 이름을 ~config라고 지음
// @Configuration -> 스프링 설정 클래스임을 알려주는 어노테이션
@Configuration
@EnableMethodSecurity // 메서드 수준 보안 활성화

public class SecurityConfig {
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// 1. 어떤 URL에 보안 걸지 & 로그인 방식 정의
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable) // 일단 개발/테스트용으로 csrf 끄기
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/", "/css/**", "/js/**", "/images/**",
					"/h2-console/**", "/users/join", // 회원가입 API는 인증 없이 허용
					"/users/join-form") // 폼 회원가입 처리
//					"/users/login") // (지금 API용 login도 쓰고 있다면)
					.permitAll() // 이 URL들은 누구나 접근 가능
					.anyRequest().authenticated() // 나머지는 로그인 필요
				)
			.formLogin(form -> form
			// 기본 로그인 폼 사용
				// .loginPage("/login") loginPage 지정 안하면, Spring 기본 로그인 페이지(/login) 자동 제공
				.loginPage("/login")
				.defaultSuccessUrl("/boards", true)
				.permitAll())
			.logout(Customizer.withDefaults()); // 로그인 성공 시 /boards 로 강제 리다이렉트

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
