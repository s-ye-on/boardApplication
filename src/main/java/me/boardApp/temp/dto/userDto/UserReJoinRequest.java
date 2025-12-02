package me.boardApp.temp.dto.userDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserReJoinRequest(
	@NotBlank
	String realName,

	@NotBlank
	@Email
	String email,

	@NotBlank
	@Size(min = 3, max = 30, message = "닉네임은 최소 3자, 최대 30자까지 가능합니다")
	String nickname,

	@NotBlank
	String password
) {
}
