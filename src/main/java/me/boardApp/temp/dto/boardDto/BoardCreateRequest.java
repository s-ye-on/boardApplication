package me.boardApp.temp.dto.boardDto;

import jakarta.validation.constraints.NotBlank;
import me.boardApp.domain.board.Board;

// data transfer object -> 택배 상자
public record BoardCreateRequest(
	@NotBlank
	String name,

	@NotBlank
	String description,

	Board.Type type
) {
}
