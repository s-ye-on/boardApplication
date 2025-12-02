package me.boardApp.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.boardApp.domain.board.Board;

// nested class를 활용한 DTO 관리
// 1. (sealed) interface + record
public sealed interface BoardRequest
	permits BoardRequest.Create, BoardRequest.Update,
	PostRequest {
		// enum class란? enumerate(열거하다) 열거형
	// enum의 재밌는 속성 -> singleton (바뀌지 않음. 애플리케이션이 종료되는 순간까지 모든 instance(메모리)가 같음)
	// enum 간의 비교 -> Object.equals(), '==' (O)

	// sealed의 핵심은 class의 enum형
	// 상속의 특징 -> 부모는 어떤 녀석이 자신을 상속하고 있는지 모름 (자식의 존재를 모른다)
	// 부모가 자식의 존재들을 모두 앎. 제약조건 -> 같은 package 상에 위치해야 한다.

	// ADT(추상 데이터 클래스) ✅
	record Create(
		@NotBlank(message = "게시판 이름은 필수 입니다")
		String name,

		@NotBlank(message = "게시판 설명은 필수 입니다")
		String description,

		@NotBlank
		Board.Type type
	) implements BoardRequest {
	}

	record Update(
		@NotBlank(message = "게시판 이름은 필수 입니다")
		@Size(min = 1, max = 50)
		String name,

		@NotBlank(message = "게시판 설명은 필수 입니다")
		@Size(min = 1, max = 50)
		String description
	) implements BoardRequest {
	}
}

// 2. class + record
//public final class BoardRequest {
//	public record Create(
//
//	) {
//	}
//}

// 3. class + class

// 4. (sealed) interface + class
//public interface BoardRequest {
//	@Data
//	class Create {
//		@NotBlank
//		String name;
//
//		@NotBlank
//		String description;
//
//		Board.Type type;
//	}
//
//}
