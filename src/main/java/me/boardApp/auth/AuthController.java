package me.boardApp.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boardApp.auth.jwt.JwtTokenProvider;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import me.boardApp.global.response.SuccessMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public record LoginResponse(Long userId, String email, String nickname, String accessToken, String message) {}

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid UserRequest.Login request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

        // 2. 비밀번호 검사
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserException(ExceptionCode.INVALID_PASSWORD);
        }

        // 3. JWT Access Token 생성
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // 4. 응답 DTO 생성
        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                accessToken,
                SuccessMessage.LOGIN_SUCCESS.getMessage()
        );

        return ResponseEntity
                .status(SuccessMessage.LOGIN_SUCCESS.getStatus())
                .body(response);
    }
		@GetMapping("/me")
		public ResponseEntity<LoginResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
			// JwtAuthenticationFilter 덕분에 여기 올 때 이미 인증된 상태
			LoginResponse response = new LoginResponse(
				userDetails.getId(),
				userDetails.getEmail(),
				userDetails.getNickname(),
				null, // 토큰은 여기서 다시 줄 필요 x
				"현재 로그인된 사용자 정보"
			);
			return ResponseEntity.ok(response);
		}
}
