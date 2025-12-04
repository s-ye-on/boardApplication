package me.boardApp.authorization;

import me.boardApp.domain.user.User;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

	public void checkOwnerOrAdmin(User resourceOwner, User currentUser) {
		boolean isOwner = resourceOwner.getId().equals(currentUser.getId());
		boolean isAdmin = currentUser.isAdmin();

		if(!isOwner &&  !isAdmin) {
			throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
		}
	}

	public void checkOwner(User resourceOwner, User currentUser) {
		boolean isOwner = resourceOwner.getId().equals(currentUser.getId());
		if(!isOwner) {
			throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
		}
	}

	public void checkUser(User currentUser) {
		if(currentUser.isAdmin()) {
			throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
		}
	}

	public void checkAdmin(User currentUser) {
		boolean isAdmin = currentUser.isAdmin();
		if(!isAdmin) {
			throw new UserException(ExceptionCode.FORBIDDEN_ADMIN);
		}
	}
}
