package me.boardApp.global.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import me.boardApp.global.validation.UniqueNickname;

public sealed interface UserRequest
	permits UserRequest.Create,
	UserRequest.Login,
	Update,
	UserRequest.UpdateNickname, UserRequest.UpdateEmail, UserRequest.UpdatePassword,
	UserRequest.ReJoin,
	UserRequest.Delete {

	record Create(
		@NotBlank(message = "이름은 필수 입니다")
		@Size(min = 3, max = 50, message = "3자 이상 50자 이하로 입력 해주세요")
		String realName,

		@NotBlank(message = "닉네임은 필수 입니다")
		//@UniqueNickname
		@Size(min = 3, max = 30, message = "닉네임은 최소 3자, 최대 30자까지 가능합니다")
		String nickName,

		@NotBlank(message = "비밀번호는 필수 입니다")
		String password,

		@NotBlank(message = "이메일은 필수 입니다")
		@Email
		String email
	) implements UserRequest {
	}

	record Login(
		@NotBlank(message = "이메일 아이디는 필수 입니다")
		@Email
		String email,

		@NotBlank(message = "비밀번호 입력은 필수 입니다")
		String password
	) implements UserRequest {
	}

	record UpdateNickname(
		@NotBlank
		@Size(min = 3, max = 30, message = "닉네임은 최소 3자, 최대 30자까지 가능합니다")
		String newNickName,

		@NotBlank
		String password

	) implements UserRequest {
	}

	record UpdateEmail(
		@NotBlank
		String password,

		@NotBlank
		@Email
		String newEmail
	) implements UserRequest {
	}

	record UpdatePassword(
		@NotBlank
		String presentPassword,

		@NotBlank
		String newPassword
	) implements UserRequest {
	}

	// 삭제는 로그인 상태에서만 가능하게 만들겠음
	record Delete(
		@NotBlank(message = "실명 입력은 필수 입니다")
		String realName,

		@NotBlank(message = "비밀번호 입력은 필수 입니다")
		String password,

		@NotBlank(message = "이메일 입력은 필수 입니다")
		@Email
		String email
	) implements UserRequest {
	}

	record ReJoin(
		@NotBlank(message = "실명 입력은 필수 입니다")
		String realName,

		@NotBlank(message = "이메일 입력은 필수 입니다")
		@Email
		String email,

		@NotBlank
		@Size(min = 3, max = 30, message = "닉네임은 최소 3자, 최대 30자까지 가능합니다")
		String nickname,

		@NotBlank
		String password
	) implements UserRequest {
	}
}
