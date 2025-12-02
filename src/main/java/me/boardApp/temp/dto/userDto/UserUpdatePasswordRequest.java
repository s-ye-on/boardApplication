package me.boardApp.temp.dto.userDto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdatePasswordRequest(
	@NotBlank
	String nickname,

	@NotBlank
	String presentPassword,

	@NotBlank
	String newPassword
) {
}
