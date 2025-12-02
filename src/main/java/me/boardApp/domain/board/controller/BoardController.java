package me.boardApp.domain.board.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.board.dto.BoardResponse;
import me.boardApp.domain.board.service.BoardService;
import me.boardApp.global.dto.request.BoardRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {
	private final BoardService boardService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BoardResponse.Create create(
		@RequestBody @Valid BoardRequest.Create request
	) {
		return boardService.create(request);
	}

	// 수동으로 직접 Pageable 생성
	// 백엔드에서 고정된 정렬 기준으로 직접 코드를 작성
	// 즉 무조건 최신순 또는 이름 오름차순 같은 고정 규칙으로 강제하는 방식
//	@GetMapping
//	public Page<BoardReadResponse> getAll() {
//		Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
//		return boardService.readAll(pageable);
//	}

	// 요청 파라미터로 정렬 제어 (더 실전적 방식)
	// 요청 파라미터는 프론트엔드 (API 호출자)가 작성
	@GetMapping
	public Page<BoardResponse.Read> findAll(
		@PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return boardService.readAll(pageable);
	}

	@GetMapping("/{name}")
	public BoardResponse.Read findByName(@PathVariable String name) {
		return boardService.readByName(name);
	}
//	@PathVariable URL 경로의 일부를 받음
//	@RequestParam 쿼리 파라미터 (? 뒤에 오는 값)을 받음
	// id 처럼 고유하게 식별자로 쓰는 것 @PathVariable
	// name처럼 검색/조건에 쓰는 값은 @RequestParam

	@GetMapping("/{id}")
	public BoardResponse.Read findById(@PathVariable Long id) {
		return boardService.readById(id);
	}

	// PutMapping vs PatchMapping
	// Put은 전체가 다 새걸로 바뀌는 것 patchMapping은 바뀐 부분만 변경
	// Put vs Patch 어차피 jpa에서 update할 때 전체 필드가 다 날아가서 업데이트되니까
	// 둘 중 아무거나 써도 상관이 없다
	@PatchMapping("/{id}")
	public BoardResponse.Update update(@PathVariable Long id, @RequestBody @Valid BoardRequest.Update request) {
		return boardService.updateBoard(id, request);
	}

	@DeleteMapping("/{id}")
//	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		boardService.delete(id);
	}
}
