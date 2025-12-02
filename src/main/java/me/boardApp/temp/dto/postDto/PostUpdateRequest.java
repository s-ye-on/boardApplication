package me.boardApp.temp.dto.postDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(
	@NotBlank(message = "작성자 입력은 필수 입니다")
	String nickName,

	@NotBlank(message = "비밀번호 입력은 필수 입니다")
	String password,

	@NotBlank(message = "제목 입력은 필수 입니다")
	@Size(max = 100)
	String title,

	@NotBlank(message = "내용 입력은 필수 입니다")
	@Size(max = 1000)
	String text
) {
}
