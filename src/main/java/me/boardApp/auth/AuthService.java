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

		// refresh token 만료 기간 계산
		LocalDateTime refreshExpiry = jwtTokenProvider.calculateRefreshExpiry();

		// 3-2 : Refresh Token DB 저장/업데이트 (만료 계산은 JwtTokenProvider에서 처리)
		RefreshToken refreshToken = refreshTokenRepository.findTopByUserOrderByIdDesc(user)
			.map(rt -> {
				rt.rotate(refreshTokenValue, refreshExpiry);
				return rt;
			})
			.orElseGet(() -> RefreshToken.create(
				user,
				refreshTokenValue,
				refreshExpiry
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

		if (refreshToken.getStatus() == RefreshToken.Status.REVOKED) {
			throw new AuthorizationException(ExceptionCode.REFRESH_REUSED);
		}

		User user = refreshToken.getUser();

		// 3. 새 Access Token 생성
		String newAccessToken = jwtTokenProvider.generateAccessToken(
			user.getId(),
			user.getEmail(),
			user.getRole().name()
		);

		// 4. rotation : refresh token 재발급
		refreshToken.revoke();
		String newRefreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());
		LocalDateTime newRefreshExpiry = jwtTokenProvider.calculateRefreshExpiry();

		RefreshToken newRefreshToken = RefreshToken.create(user, newRefreshTokenValue, newRefreshExpiry);

		refreshTokenRepository.save(refreshToken); // 옛날 토큰 revoked 저장
		refreshTokenRepository.save(newRefreshToken); // 새로운 토큰 active 저장

		return new AuthResponse.Login(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			newAccessToken,
			newRefreshTokenValue,
			"Access Token 재발급 성공"
		);
	}
}
