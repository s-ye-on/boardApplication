package me.boardApp.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boardApp.auth.AuthService;
import me.boardApp.auth.dto.AuthResponse;
import me.boardApp.auth.dto.RefreshRequest;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.global.response.SuccessMessage;
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
	public ResponseEntity<AuthResponse.Login> login(@RequestBody @Valid UserRequest.Login request) {
		AuthResponse.Login response = authService.login(request);

		return ResponseEntity
			.status(SuccessMessage.LOGIN_SUCCESS.getStatus())
			.body(response);
	}

	// refresh 요청
	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse.Login> refresh(@RequestBody @Valid RefreshRequest request) {
		AuthResponse.Login response = authService.refresh(request);
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
