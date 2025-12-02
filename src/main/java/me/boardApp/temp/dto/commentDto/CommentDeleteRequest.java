package me.boardApp.temp.dto.commentDto;

import jakarta.validation.constraints.NotBlank;

// 작성자 기준으로 삭제하기위해 사용
public record CommentDeleteRequest(
	@NotBlank
	String writer,

	@NotBlank
	String password
) {
}
