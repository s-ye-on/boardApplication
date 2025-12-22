package me.boardApp.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boardApp.auth.AuthService;
import me.boardApp.auth.dto.AuthResponse;
import me.boardApp.auth.dto.RefreshRequest;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.global.response.SuccessMessage;
import me.boardApp.log.ClientContextFilter;
import me.boardApp.log.dto.ClientContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 로그인/내 정보 API
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	// 현재 여기 있는 login은 jwt 기반 로그인, UserController에 있는 login은 세션/응답 dto 기반의 로그인 API
	// 이메일/비밀번호로 JWT 발급
	@PostMapping("/login")
	public ResponseEntity<AuthResponse.Login> login(
		@RequestBody @Valid UserRequest.Login request,
		HttpServletRequest servletRequest
	) {
		// clientContext는 필터에서만 생성하고,
		// Controller가 ClientContext를 꺼냄
		// Service 는 HTTP를 모른다
		// clientContext는 그냥 "환경 정보 DTO" 일 뿐
		// 필터가 넣는 키니까 필터가 상수 정의 후 가져다 씀
		// 필터에서 분명 ClientContext 형으로 저장했는데 Controller에서 왜 형변환을 해줘야하나?
		// -> getAttribute()는 무조건 Object를 반환함
		// ClientContext인지 모름 -> 명시적 형변환 필요
		// 재변환이 아니라, 같은 객체를 같은 참조로 타입만 알려주는 것 -> 이 Object를 ClientContext로 다뤄도 된다고 컴파일러에게 알려준 것
		ClientContext context = (ClientContext) servletRequest.getAttribute(ClientContextFilter.CLIENT_CONTEXT_KEY);

		AuthResponse.Login response = authService.login(request, context);

		return ResponseEntity
			.status(SuccessMessage.LOGIN_SUCCESS.getStatus())
			.body(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(
		@RequestBody RefreshRequest request,
		HttpServletRequest servletRequest) {

		ClientContext context = (ClientContext) servletRequest.getAttribute(ClientContextFilter.CLIENT_CONTEXT_KEY);

		authService.logout(request.refreshToken(), context);

		return ResponseEntity.ok(
			SuccessMessage.LOGOUT_SUCCESS.getMessage()
		);
	}

	// refresh 요청
	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse.Login> refresh(
		@RequestBody @Valid RefreshRequest request,
		HttpServletRequest servletRequest) {

		ClientContext context = (ClientContext) servletRequest.getAttribute(ClientContextFilter.CLIENT_CONTEXT_KEY);

		AuthResponse.Login response = authService.refresh(request, context);

		return ResponseEntity.ok(response);
	}

	// 현재 로그인 유저 정보 반환
	@GetMapping("/me")
	public ResponseEntity<AuthResponse.Me> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
		// JwtAuthenticationFilter 덕분에 여기 올 때 이미 인증된 상태
		AuthResponse.Me response = new AuthResponse.Me(
			userDetails.getId(),
			userDetails.getEmail(),
			userDetails.getNickname(),
			"현재 로그인된 사용자 정보"
		);

		return ResponseEntity.ok(response);
	}
}
