package me.boardApp.domain.post.dto;

public sealed interface PostResponse
	permits PostResponse.Create, PostResponse.Read, PostResponse.Update {

	record Create(
		Long id,
		String boardName,
		String title,
		String writer
	) implements PostResponse {
	}

	record Read(
		String title,
		String boardName,
		String writer,
		Long views
	) implements PostResponse {
	}

	record Update(
		Long id,
		String writer,
		String title,
		String text
	) implements PostResponse {
	}
}
