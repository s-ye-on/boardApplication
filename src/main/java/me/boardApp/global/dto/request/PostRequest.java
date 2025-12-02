package me.boardApp.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// adt의 성질을 이용하여 이 dto는 특정 요청/응의 유형이다~ 라는걸 가시적으로 보여주기 위해 extends boardRequest해줌
// 여기서는 뭔가 추가적인 관계가 있는 경우를 말한거긴 함
// board와 post의 밀접한 관계성이 있는 경우 추가적인 상속을 해서 관계를 표현해줄 수 있는거
// 추가적인 관계 board가 존재해야 post가 존재할 수 있는 그런 조건을 말함
// 게시글 요청은 결국 게시판 요청의 하위 개념임을 상속을 통해 표현
public sealed interface PostRequest extends BoardRequest
	permits PostRequest.Create, PostRequest.Update, PostRequest.Delete,
	CommentRequest {
	record Create(
		@NotBlank(message = "게시판 입력은 필수 입니다")
		Long boardId,

		@NotBlank(message = "작성자 입력은 필수 입니다")
		String nickname,

		@NotBlank(message = "제목 입력은 필수 입니다")
		@Size(max = 50, message = "제목은 최대 50자까지 가능합니다")
		String title,

		@NotBlank(message = "내용 입력은 필수 입니다")
		@Size(max= 1000, message = "내용은 최대 1000자까지 가능합니다")
		String text
	)implements PostRequest{}

	record Update(
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

	)
	implements PostRequest{}

	record Delete(
		@NotBlank(message = "작성자 입력은 필수 입니다")
		String nickName,

		@NotBlank(message = "비밀번호 입력은 필수 입니다")
		String password

//	@NotBlank(message = "작성자 입력은 필수 입니다")
//	String writer
//
	)implements PostRequest{}
}
