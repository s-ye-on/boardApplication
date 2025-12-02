package me.boardApp.domain.user.dto;

public sealed interface UserResponse permits UserResponse.Login{
	record Login(Long id, String nickname, String message) implements UserResponse {
		public Login withMessage(String message) {
			return new Login(id, nickname, message);
		}
	}
}
