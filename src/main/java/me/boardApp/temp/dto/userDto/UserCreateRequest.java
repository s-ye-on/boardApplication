package me.boardApp.temp.dto.userDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
	@NotBlank(message = "이름은 필수 입니다")
	@Size(min = 3, max = 50, message = "3자 이상 50자 이하로 입력 해주세요")
	String name,

	@NotBlank
	@Size(min = 3, max = 30, message = "닉네임은 최소 3자, 최대 30자까지 가능합니다")
	String nickName,

	@NotBlank(message = "비밀번호는 필수 입니다")
	String password,

	@NotBlank(message = "이메일은 필수 입니다")
	@Email
	String email
) {
}
