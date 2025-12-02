package me.boardApp.temp.dto.userDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDeleteRequest(
	@NotBlank
	String nickname,

	@NotBlank
	String realName,

	@NotBlank
	String password,

	@NotBlank
	@Email
	String email
) {
}
