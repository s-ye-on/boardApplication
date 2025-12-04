package me.boardApp.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boardApp.auth.jwt.JwtTokenProvider;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import me.boardApp.global.response.SuccessMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
