package me.boardApp.domain.board.dto;

import me.boardApp.domain.board.Board;

import java.time.LocalDateTime;

public sealed interface BoardResponse
	permits BoardResponse.Create,
	BoardResponse.Read,
	BoardResponse.Update {
	record Create(
		Long id,
		String name,
		String description,
		Board.Type type,
		LocalDateTime createTime
	) implements BoardResponse {
	}

	record Read(
		Long id,
		String name,
		Board.Type type,
		String description
	) implements BoardResponse {
		// 정적 팩토리 메서드
		public static BoardResponse.Read from(Board board) {
			return new BoardResponse.Read(board.getId(), board.getName(), board.getType(), board.getDescription());
		}
	}

	record Update(
		String name,
		String description,
		LocalDateTime createTime,
		LocalDateTime updateTime
	) implements BoardResponse {
	}
}
