package me.boardApp.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 이런식으로 하면 UserRequest.Update.Nickname
// 이런식으로도 둘 수 있음 -> 좀 더 깔끔하긴 함
// 일단 현재 이건 사용하진 않음 이렇게하면 더 깔끔하게 갈 수도 있다 하는걸 보여주기 위해 존재
public sealed interface Update extends UserRequest
	permits Update.Nickname, Update.Password {
	record Nickname(
		@NotBlank
		String presentNickname,

		@NotBlank
		@Size(min = 3, max = 30, message = "닉네임은 최소 3자, 최대 30자까지 가능합니다")
		String newNickName,

		@NotBlank
		String password
	) implements Update {
	}

	record Password(
		@NotBlank
		String nickname,

		@NotBlank
		String presentPassword,

		@NotBlank
		String newPassword
	) implements Update {
	}
}
