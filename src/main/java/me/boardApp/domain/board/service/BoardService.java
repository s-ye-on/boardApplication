package me.boardApp.domain.board.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.dto.BoardResponse;
import me.boardApp.domain.post.service.PostService;
import me.boardApp.global.exception.BoardException;
import me.boardApp.domain.board.BoardRepository;
import me.boardApp.global.dto.request.BoardRequest;
import me.boardApp.global.exception.ExceptionCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

// 기본적인 CRUD는 BoardRepository에서 제공함
// BoardService는 비즈니스 규칙이 반영된 CRUD를 제공
@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {
	private final BoardRepository boardRepository;
	private final PostService postService;

	@PostConstruct
	public void init() {
		if (!boardRepository.existsByType(Board.Type.TEMPORARY)) {
			boardRepository.save(new Board("임시 게시판", "임시 게시판입니다", Board.Type.TEMPORARY));
		}
	}

	// Create
	// PRG : post -> redirect -> get
	// 그래서 생성에 대해서 딱히 응답을 내려줄 필요는 없다.
	public BoardResponse.Create create(BoardRequest.Create request) {
		if (request.type().isTemporary()) {
			throw new BoardException(ExceptionCode.DUPLICATE_TEMP_BOARD);
		}

		//1. 엔티티 생성
		Board board = new Board(
			request.name(),
			request.description(),
			request.type()
		);

		//2. 저장 (id, createTime같은 값 채워짐)
		boardRepository.save(board);

		//3. Response 리턴
		return new BoardResponse.Create(
			board.getId(),
			board.getName(),
			board.getDescription(),
			board.getType(),
			board.getCreatedDate()
		);
	}

	//Read
	// 여기서 readRequest 안쓴 이유는 ddd 스타일에 더 맞음
	// RESTful 스타일로 충분함
	// Get은 리소스를 조회하는 요청이라 body 잘 안씀
	// DTO가 필요한 경우 : 1. Post/Put -> body에 들어오는 JSON 데이터를 객체로 매핑할 때
	// 2. Response : Entity를 그대로 노출하지 않고 필요한 정보만 담아 반환할 때
	public BoardResponse.Read readById(Long id) {
		Board result = boardRepository.findById(id)
			.orElseThrow(() -> new BoardException(ExceptionCode.NOT_FOUND_BOARD));

		return toReadResponse(result);
	}

	private Board getById(Long id) {
		return boardRepository.findById(id)
			.orElseThrow(()-> new BoardException(ExceptionCode.NOT_FOUND_BOARD));
	}

	public BoardResponse.Read readByName(String name) {
		Board result = boardRepository.findByName(name)
			.orElseThrow(() -> new BoardException(ExceptionCode.NOT_FOUND_BOARD));

		return toReadResponse(result);
	}

	public Page<BoardResponse.Read> readAll(Pageable pageable) {
//		return boardRepository.findAll().stream()
//			.map(this::toReadResponse)
//			.toList();
		// page로 변경
		// 스프링 데이터 jpa에서 이미 Pageable을 인자로 받는 findAll(Pageable pageable) 버전을 자동으로 지원해줌
		return boardRepository.findAll(pageable)
			.map(this::toReadResponse);
	}

	private BoardResponse.Read toReadResponse(Board board) {
		return new BoardResponse.Read(
			board.getId(),
			board.getName(),
			board.getType(),
			board.getDescription());
	}

	// Update
	/// todo : board update 관리자가 가능하게 만들어야함
	public BoardResponse.Update updateBoard(Long id, BoardRequest.Update request) {
		Board target = getById(id);

//		Optional.ofNullable(T value) : 주어진 값이 null인 경우 빈 Optional을, null이 아닌 경우 값을 포함하는 Optional 반환
		target.update(request.name(), request.description());
		return toUpdateResponse(target);
	}

	public BoardResponse.Update toUpdateResponse(Board board) {
		return new BoardResponse.Update(
			board.getName(),
			board.getDescription(),
			board.getCreatedDate(),
			board.getLastModifiedDate());
	}

	// Delete
	/// todo : delete도 지금 보면 누구나 게시판을 삭제할 수 있음
	public void delete(Long id) {
		Board target = getById(id);

		if (target.getType() == Board.Type.TEMPORARY) {
			throw new BoardException(ExceptionCode.CANNOT_DELETE_TEMP_BOARD);
		}

		// IllegalStateException : 메서드를 호출하려했지만, 객체의 상태가 메서드 호출을 허용하지 않을 경우에 사용
		// IllegalArgumentException : 메서드에 전달된 인자가 잘못되었을 때 사용

		// 임시 게시판 찾기
		Board tempBoard = boardRepository.findByType(Board.Type.TEMPORARY)
			.orElseThrow(() -> new BoardException(ExceptionCode.NOT_FOUND_TEMP_BOARD));

		// 게시글 이동
		postService.migrate(target, tempBoard);

		//게시판 삭제
		// db에서
		boardRepository.delete(target);
		// 컬렉션에서
//		target.getPosts().clear();
	}
}
