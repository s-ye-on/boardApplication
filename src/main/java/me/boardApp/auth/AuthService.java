package me.boardApp.auth;

import lombok.RequiredArgsConstructor;
import me.boardApp.auth.dto.RefreshRequest;
import me.boardApp.auth.jwt.JwtTokenProvider;
import me.boardApp.auth.dto.AuthResponse;
import me.boardApp.auth.token.RefreshToken;
import me.boardApp.auth.token.RefreshTokenRepository;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.global.exception.AuthorizationException;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import me.boardApp.global.response.SuccessMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
	private static final int ONE_WEEK = 604800;

	// 여기서도 단순 조회라 commonService 써도 되지만,
	// 인증(Auth)도 결국 "User 도메인 위에 올라가는 별도의 서브 도메인"이라 볼 수 있고,
	// 그렇기에 그냥 여기선 한 번 userRepository를 써보겠음
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthResponse.Login login(UserRequest.Login request) {
		// 1. 이메일로 사용자 조회
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		// 2. 비밀번호 검증
		user.validatePassword(request.password(), passwordEncoder);

		// 3. JWT Access Token 생성
		String accessToken = jwtTokenProvider.generateAccessToken(
			user.getId(),
			user.getEmail(),
			user.getRole().name()
		);

		// 3-1. 추가 : Refresh Token 생성
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());

		// 3-2 : Refresh Token DB 저장/업데이트
		// .plusSeconds(ONE_WEEK)를 지금 서비스에서 해주고 있지만 엔티티쪽에서 해주면 더 깔끔할 것 같기도 함 고민 해보자
		RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
			.map(rt -> {
				rt.updateToken(refreshTokenValue, LocalDateTime.now().plusSeconds(ONE_WEEK));
				return rt;
			})
			.orElseGet(() -> RefreshToken.create(
				user,
				refreshTokenValue,
				LocalDateTime.now().plusSeconds(ONE_WEEK)
			));

		refreshTokenRepository.save(refreshToken);

		// 4. 응답 dto 생성
		AuthResponse.Login loginResponse = new AuthResponse.Login(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			accessToken,
			refreshTokenValue,
			SuccessMessage.LOGIN_SUCCESS.getMessage()
		);

		return loginResponse;
	}

	public AuthResponse.Login refresh(RefreshRequest request) {
		String refreshTokenValue = request.refreshToken();

		// JWT 자체 서명/만료 검증
		jwtTokenProvider.validateToken(refreshTokenValue);

		// 1. DB에서 refreshToken 조회
		RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
			.orElseThrow(() -> new AuthorizationException(ExceptionCode.TOKEN_INVALID));

		// 2. 만료 여부 체크
		if (refreshToken.isExpired()) {
			throw new AuthorizationException(ExceptionCode.TOKEN_EXPIRED);
		}

		User user = refreshToken.getUser();

		// 3. 새 Access Token 생성
		String newAccessToken = jwtTokenProvider.generateAccessToken(
			user.getId(),
			user.getEmail(),
			user.getRole().name()
		);

		// 4. 필요하다면 여기서 refreshToken도 재발급. 하지만 지금은 유지로 선택
		return new AuthResponse.Login(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			newAccessToken,
			refreshTokenValue,
			"Access Token 재발급 성공"
		);
	}
}
