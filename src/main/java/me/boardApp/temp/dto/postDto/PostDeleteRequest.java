package me.boardApp.temp.dto.postDto;

import jakarta.validation.constraints.NotBlank;

public record PostDeleteRequest(
	@NotBlank(message = "작성자 입력은 필수 입니다")
	String nickName,

	@NotBlank(message = "비밀번호 입력은 필수 입니다")
	String password

//	@NotBlank(message = "작성자 입력은 필수 입니다")
//	String writer
//
) {
}
