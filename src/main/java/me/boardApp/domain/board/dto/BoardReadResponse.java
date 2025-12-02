//package me.boardApp.domain.board.dto;
//
//import me.boardApp.domain.board.Board;
//import me.boardApp.global.dto.request.BoardRequest;
//
//// Read는 request가 필요 없음
//public record BoardReadResponse(
//	Long id,
//	String name,
//	Board.Type type,
//	String description
//) {
//	// 정적 팩토리 메서드
//	public static BoardReadResponse from (Board board) {
//		return new BoardReadResponse(board.getId(), board.getName(), board.getType(), board.getDescription());
//	}
//}
