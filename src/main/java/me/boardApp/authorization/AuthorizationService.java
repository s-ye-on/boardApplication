package me.boardApp.authorization;

import me.boardApp.domain.user.User;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import org.springframework.stereotype.Service;

// 도메인 권한 체크
@Service
public class AuthorizationService {
	// 권한 검증(Authorization)이 필요한 이유
	// 엔티티에 작성된 user와 현재 로그인한 user(=currentUser)가 동일인인지 비교해야함

	// 기존 로직이 인증(Authentication)이라면,
	// 닉네임 & 비밀번호 기반 본인 확인
	// 말 그대로 너 A맞지?

	// 권한 로직(Authorization)은
	// 현재 로그인 주체가 글을 수정할 권한이 있는가?
	// 작성자 본인인가? or 관리자(admin)인가?

	public void checkOwnerOrAdmin(User resourceOwner, User currentUser) {
		boolean isOwner = resourceOwner.getId().equals(currentUser.getId());
		boolean isAdmin = currentUser.isAdmin();

		if (!isOwner && !isAdmin) {
			throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
		}
	}

	public void checkOwner(User resourceOwner, User currentUser) {
		boolean isOwner = resourceOwner.getId().equals(currentUser.getId());
		if (!isOwner) {
			throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
		}
	}

	public void checkUser(User currentUser) {
		if (currentUser.isAdmin()) {
			throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
		}
	}

	public void checkAdmin(User currentUser) {
		boolean isAdmin = currentUser.isAdmin();
		if (!isAdmin) {
			throw new UserException(ExceptionCode.FORBIDDEN_ADMIN);
		}
	}
}
