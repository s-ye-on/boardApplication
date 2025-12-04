package me.boardApp.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 나중에 댓글이 유저 프로필에 댓글을 단다거나, 이벤트 댓글이 있거나 할 때는 그냥 독립적인 CommentRequest로 두는 것이 좋음
// 댓글이 다른 컨텍스트에서 필요하다는 요구 사항이 생기면 그때 CommentRequest를 독립
// YAGNI
public sealed interface CommentRequest extends PostRequest
permits CommentRequest.Create, CommentRequest.Update,
	CommentRequest.DeleteByAdmin , CommentRequest.Delete {
	record Create(
		@NotBlank(message = "댓글 본문은 필수 입니다")
		@Size(max = 500, message = "댓글은 500자 이하로 입력해주세요")
		String comment
	)implements CommentRequest {}

	record Update(
		@NotBlank(message = "비밀번호 입력은 필수 입니다")
		String password,

		@NotBlank(message = "댓글 본문은 필수 입니다")
		@Size(max = 500, message = "500자 이하로 입력해주세요")
		String comment
	)
	implements CommentRequest{}

	record Delete(
		@NotBlank(message = "비밀번호 입력은 필수 입니다")
		String password
	)
		implements CommentRequest{}

	record DeleteByAdmin(
		@NotBlank(message = "작성자 입력은 필수 입니다")
		String writer,

		@NotBlank(message = "관리자 비밀번호 입력은 필수 입니다")
		String password
	)
	implements CommentRequest{}
}
