package me.boardApp.temp.dto.postDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
	@NotBlank(message = "게시판 입력은 필수 입니다")
	Long boardId,

	@NotBlank(message = "작성자 입력은 필수 입니다")
	String nickname,

//	@NotBlank(message = "작성자 입력은 필수 입니다")
//	String writer,
//
//	@NotBlank(message = "비밀번호 입력은 필수 입니다")
//	String password,

	@NotBlank(message = "제목 입력은 필수 입니다")
	@Size(max = 50, message = "제목은 최대 50자까지 가능합니다")
	String title,

	@NotBlank(message = "내용 입력은 필수 입니다")
	@Size(max= 1000, message = "내용은 최대 1000자까지 가능합니다")
	String text
) {
}
