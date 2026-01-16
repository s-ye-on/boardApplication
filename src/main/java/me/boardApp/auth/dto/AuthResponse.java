package me.boardApp.auth.dto;

public sealed interface AuthResponse permits AuthResponse.Login, AuthResponse.Me {
	record Login(
		Long userId,
		String email,
		String nickname,
		String accessToken,
		String refreshToken,
		String message
	) implements AuthResponse {
	}

	record Me(
		Long userId,
		String email,
		String nickname,
		String message
	) implements AuthResponse {
	}
}
