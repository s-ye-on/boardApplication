package me.boardApp.temp.dto.commentDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
	@NotBlank
	String writer,

	@NotBlank
	String password,

	@NotBlank
	@Size(max = 500, message = "500자 이하로 입력해주세요")
	String comment
) {
}
