package me.boardApp.temp.dto.boardDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardUpdateRequest(
	@NotBlank
	@Size(min = 1, max = 50)
	String name,

	@NotBlank
	@Size(min = 1, max = 50)
	String description
) {
}
