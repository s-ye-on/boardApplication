package me.boardApp.temp.dto.commentDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
	@NotBlank(message = "작성자는 필수 입니다")
	String writer,

	@Size(max = 500, message = "댓글은 500자 이하로 입력해주세요")
	String comment
) {
}
