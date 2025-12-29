package me.boardApp.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import me.boardApp.log.SecurityEventService;
import me.boardApp.log.SecurityEventType;
import me.boardApp.log.dto.ClientContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthService {
	private static final int LOGIN_FAIL_THRESHOLD = 5;

	// 여기서도 단순 조회라 commonService 써도 되지만,
	// 인증(Auth)도 결국 "User 도메인 위에 올라가는 별도의 서브 도메인"이라 볼 수 있고,
	// 그렇기에 그냥 여기선 한 번 userRepository를 써보겠음
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final SecurityEventService securityEventService;

	public AuthResponse.Login login(UserRequest.Login request, ClientContext clientContext) {
		log.info("로그인 시도 : userEmail = {}", request.email());

		// 1. 이메일로 사용자 조회
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> {
				securityEventService.record(
					SecurityEventType.LOGIN_FAIL_UNKNOWN_USER,
					null,
					clientContext
				);
				return new UserException(ExceptionCode.USER_VALIDATION_FAILED);
			});

		// 여기에 user 계정의 status가 lock이라면 로그인 실패 후 본인인증 시키게 만들자
		if (!user.validActivate()) {
			log.warn("잠긴 계정 로그인 시도 userEmail = {}", request.email());

			// 보안 이벤트는 비즈니스 판단이 일어난 지점에서 남긴다
			securityEventService.record(
				SecurityEventType.LOCKED_ACCOUNT_LOGIN_ATTEMPT,
				user.getId(),
				clientContext
			);

			throw new AuthorizationException(ExceptionCode.LOCKED_ACCOUNT);
		}

		// 2. 비밀번호 검증
		try {
			user.validatePassword(request.password(), passwordEncoder);

			// 로그인 성공 시 실패 횟수 초기화
			user.resetLoginFailCount();
		} catch (UserException e) {
			user.increaseLoginFailCount();

			securityEventService.record(
				SecurityEventType.LOGIN_FAIL,
				user.getId(),
				clientContext
			);

			if(user.isLockThresholdExceeded(LOGIN_FAIL_THRESHOLD)) {
				user.lock();

				// 로그인 시도 횟수 초과 기록
				securityEventService.record(
					SecurityEventType.LOGIN_FAIL_THRESHOLD_EXCEEDED,
					user.getId(),
					clientContext
				);

				// 계정 잠굼 기록
				securityEventService.record(
					SecurityEventType.ACCOUNT_LOCKED,
					user.getId(),
					clientContext
				);
			}

			throw e;
		}

		// 3. JWT Access Token 생성
		String accessToken = jwtTokenProvider.generateAccessToken(
			user.getId(),
			user.getEmail(),
			user.getRole().name()
		);

		// 4. 기존 refresh token 전부 REVOKE
		///  todo : 이 부분은 deviceId, revoke 사유 중 하나 만들어야 함
		refreshTokenRepository.findAllByUserAndStatus(user, RefreshToken.Status.ACTIVE)
			.forEach(RefreshToken::revoke);

		// 5. 새 refresh token 생성
		String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());

		// refresh token 만료 기간 계산
		LocalDateTime refreshExpiry = jwtTokenProvider.calculateRefreshExpiry();

		// login 시 새 refresh 토큰으로 기존것을 덮는 방식이었음. 재사용 감지 불가!
		// Refresh Token DB 저장/업데이트 (만료 계산은 JwtTokenProvider에서 처리)
//		RefreshToken refreshToken = refreshTokenRepository.findTopByUserOrderByIdDesc(user)
//			.map(rt -> {
//				rt.rotate(refreshTokenValue, refreshExpiry);
//				return rt;
//			})
//			.orElseGet(() -> RefreshToken.create(
//				user,
//				refreshTokenValue,
//				refreshExpiry
//			));
		RefreshToken refreshToken = RefreshToken.create(
			user,
			refreshTokenValue,
			refreshExpiry
		);

		refreshTokenRepository.save(refreshToken);

		// 6. 응답 dto 생성
		AuthResponse.Login loginResponse = new AuthResponse.Login(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			accessToken,
			refreshTokenValue,
			SuccessMessage.LOGIN_SUCCESS.getMessage()
		);

		// 로그 기록
		securityEventService.record(
			SecurityEventType.LOGIN_SUCCESS,
			user.getId(),
			clientContext
		);

		return loginResponse;
	}

	public void logout(String refreshTokenValue, ClientContext clientContext) {

		// 1. refresh token JWT 자체 검증
		jwtTokenProvider.validateToken(refreshTokenValue);

		// 2. DB에서 refresh token 조회
		RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
			.orElseThrow(() -> {
				securityEventService.record(
					SecurityEventType.LOGOUT_FAILED_INVALID_TOKEN,
					// 여기서 userId를 null로 준 이유는
					// db에 없거나 유효하지 않으면 어떤 사용자에 속한 요청인지 특정할 수 없음
					// 실제 사용자 ID를 확정할 수 있는 경우(토큰 조회 성공)엔 그때 userId를 기록하는게 맞음
					null,
					clientContext
				);
				return new AuthorizationException(ExceptionCode.TOKEN_INVALID);
			});

		// 3. 이미 revoke 상태면 그대로 종료 (idempotent)
		if (refreshToken.getStatus() == RefreshToken.Status.REVOKED) {
			return;
		}

		// 4. revoke 처리
		refreshToken.revoke();
		refreshTokenRepository.save(refreshToken);

		securityEventService.record(
			SecurityEventType.LOGOUT_SUCCESS,
			refreshToken.getUser().getId(),
			clientContext
		);
	}

	// 나중에 관리자 기능/ 비밀번호 변경 시 사용하기 위해 만들어둠
	public void logoutAll(User user) {
		refreshTokenRepository.findAllByUserAndStatus(user, RefreshToken.Status.ACTIVE)
			.forEach(RefreshToken::revoke);
	}

	public AuthResponse.Login refresh(RefreshRequest request, ClientContext clientContext) {
		String refreshTokenValue = request.refreshToken();

		// JWT 자체 서명/만료 검증
		jwtTokenProvider.validateToken(refreshTokenValue);

		// 1. DB에서 refreshToken 조회
		RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
			.orElseThrow(() -> new AuthorizationException(ExceptionCode.TOKEN_INVALID));

		User user = refreshToken.getUser();

		// refresh에서도 LOCK된 계정은 막는게 좋아보임
		// 공격자가 refresh만 계속 찌르는거 방지
		if (!user.validActivate()) {
			log.warn("잠긴 계정 refresh 시도 userId={}", user.getId());

			securityEventService.record(
				SecurityEventType.LOCKED_ACCOUNT_REFRESH_ATTEMPT,
				user.getId(),
				clientContext
			);

			throw new AuthorizationException(ExceptionCode.LOCKED_ACCOUNT);
		}

		// 2. 만료 여부 체크
		if (refreshToken.isExpired()) {
			log.info("Refresh token 만료 userId = {}", user.getId());
			throw new AuthorizationException(ExceptionCode.TOKEN_EXPIRED);
		}

		// refresh token reuse 체크 추가적으로 이 상황에선 토큰이 탈취됐다보고 계정을 잠구고 본인인증을 요구해보자
		// 만료 여부를 먼저 체크하면 여기까지 로직이 안올 수 있는데 내 의도와 부합
		// 요청 refresh token이 정상적으로 기간이 만료됐을 수도 있음.
		// 근데 만료되지 않았는데 재사용 됐다? -> 탈취됐다고 봐야 함
		// 여기에서 계정을 잠구자
		/*
		사용자가 기기 A를 먼저 사용해서 login -> Access Token A, Refresh Token A 발급
		다음으로 사용자가 기기 B를 사용해서 login -> Access Token B, Refresh Token B 발급 -> Refresh Token A REVOKED
		사용자가 다시 A를 사용해 refresh() 요청 -> 재사용 감지로 인해 계정 잠금
		공격이 아니지만 지금 공격으로 오해하고 계정이 잠기는 상황
		해결책 3
		1. 지금 정책 유지
			- 동시 로그인 불가 -> 마지막 로그인만 유효
			- A 기기에서 refresh 시
				- 계정 LOCK X
				- O "다른 기기에서 로그인되어 세션이 종료되었습니다"
				- TOKEN_REVOKED_BY_LOGIN 같은 코드

		2. 기기별 Refresh Token 실무에서 제일 많이 사용
			- Refresh Token = 세션
			- 세션은 기기 단위
			User 1
 			├─ Session A (deviceId=A)
 			├─ Session B (deviceId=B)

 			이 경우 :
 			- A에서 refresh -> A 토큰만 revoke/rotate
 			- B는 영향 없음
 			- 진짜 reuse만 공격

 		3. Refresh Token Family (보안 최상)
 			- 하나의 refresh token이 연속적으로 이어짐
 			- rotation 시 이전 토큰만 revoke
 			- 같은 family에서 두 갈래 사용되면 공격
 			이건 :
 			- OAuth2
 			- 금융권
 			- google 계열
		 */
		// 보안 정책을 1로 유지하려면 : REVOKED_BY_ROTATION , REVOKED_BY_LOGIN 로 분리해야 함
		if (refreshToken.getStatus() == RefreshToken.Status.REVOKED) {
			user.lock();

			log.warn("Refresh Token 재사용 감지 -> 계정 잠금 userId ={}", refreshToken.getUser().getId());

			securityEventService.record(
				SecurityEventType.REFRESH_REUSED,
				user.getId(),
				clientContext
			);

			// refresh token 전부 revoke
			refreshTokenRepository.revokeAllByUser(refreshToken.getUser());

			throw new AuthorizationException(ExceptionCode.REFRESH_REUSED);
		}

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
