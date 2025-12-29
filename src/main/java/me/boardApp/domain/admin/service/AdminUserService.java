package me.boardApp.domain.admin.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import me.boardApp.log.SecurityEventService;
import me.boardApp.log.SecurityEventType;
import me.boardApp.log.dto.ClientContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

	private final UserRepository userRepository;
	private final SecurityEventService securityEventService;

	public void unlockUser(Long userId, ClientContext clientContext) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		// soft delete로 인해 INACTIVATION된 계정
		if (user.getStatus() == User.Status.INACTIVATION) {
			throw new UserException(ExceptionCode.INACTIVATION_ACCOUNT);
		}

		// 잠긴 계정인지 확인
		if (!user.isLocked()) {
			throw new UserException(ExceptionCode.NOT_LOCKED_ACCOUNT);
		}

		user.unlock();

		securityEventService.record(
			SecurityEventType.ACCOUNT_UNLOCKED_BY_ADMIN,
			user.getId(),
			clientContext
		);
	}
}
