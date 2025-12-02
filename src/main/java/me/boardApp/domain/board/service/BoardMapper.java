package me.boardApp.domain.board.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.dto.BoardResponse;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardMapper {
	public static BoardResponse.Read toReadResponse(Board board) {
		return new BoardResponse.Read(
			board.getId(),
			board.getName(),
			board.getType(),
			board.getDescription());
	}

	public static BoardResponse.Update toUpdateResponse(Board board) {
		return new BoardResponse.Update(board.getName(), board.getDescription(), board.getCreatedDate(), board.getLastModifiedDate());
	}
}
