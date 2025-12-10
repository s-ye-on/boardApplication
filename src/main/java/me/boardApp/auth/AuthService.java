package me.boardApp.auth;

import lombok.RequiredArgsConstructor;
import me.boardApp.auth.jwt.JwtTokenProvider;
import me.boardApp.auth.dto.AuthResponse;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import me.boardApp.global.response.SuccessMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
	// 여기서도 단순 조회라 commonService 써도 되지만,
	// 인증(Auth)도 결국 "User 도메인 위에 올라가는 별도의 서브 도메인"이라 볼 수 있고,
	// 그렇기에 그냥 여기선 한 번 userRepository를 써보겠음
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthResponse.Login login(UserRequest.Login request) {
		// 1. 이메일로 사용자 조회
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(()-> new UserException(ExceptionCode.NOT_FOUND_USER));

		// 2. 비밀번호 검증
		user.validatePassword(request.password(), passwordEncoder);

		// 3. JWT Access Token 생성
		String accessToken = jwtTokenProvider.generateAccessToken(
			user.getId(),
			user.getEmail(),
			user.getRole().name()
		);

		// 4. 응답 dto 생성
		AuthResponse.Login loginResponse = new AuthResponse.Login(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			accessToken,
			SuccessMessage.LOGIN_SUCCESS.getMessage()
		);

		return loginResponse;
	}
}
