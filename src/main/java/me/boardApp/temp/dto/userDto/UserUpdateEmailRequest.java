package me.boardApp.temp.dto.userDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdateEmailRequest(
	@NotBlank
	String nickname,

	@NotBlank
	String password,

	@NotBlank
	@Email
	String newEmail
) {
}
