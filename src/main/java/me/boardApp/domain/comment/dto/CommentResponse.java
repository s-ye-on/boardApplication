package me.boardApp.domain.comment.dto;

import java.time.LocalDateTime;

public sealed interface CommentResponse
	permits CommentResponse.Create, CommentResponse.Read, CommentResponse.Update {
	record Create(
		Long id,
		String writer,
		String comment,
		LocalDateTime createTime
	) implements CommentResponse {
	}

	record Read(
		Long id,
		String writer,
		String comment,
		LocalDateTime createTime,
		LocalDateTime updateTime
	) implements CommentResponse {
	}

	record Update(
		Long id,
		String writer,
		String comment,
		LocalDateTime createTime,
		LocalDateTime updateTime
	) implements CommentResponse {
	}
}
