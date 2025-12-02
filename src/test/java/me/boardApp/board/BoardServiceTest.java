package me.boardApp.board;

import jakarta.transaction.Transactional;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.dto.BoardCreateResponse;
import me.boardApp.domain.board.dto.BoardReadResponse;
import me.boardApp.domain.board.dto.BoardUpdateResponse;
import me.boardApp.domain.board.BoardRepository;
import me.boardApp.domain.board.service.BoardService;
import me.boardApp.global.dto.request.BoardRequest;
import me.boardApp.global.exception.BoardException;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.post.PostRepository;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import org.hibernate.query.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class BoardServiceTest {
	@Autowired
	private BoardService boardService;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private PostRepository postRepository;

	User user;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void 유저_생성(){
		user = new User("최승연", "얍얍", "1234", "csy03178@naver.com");
		userRepository.save(user);
	}

	@Test
	void 게시판_생성_성공() {
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);
		BoardCreateResponse boardCreate = boardService.create(createRequest);
		Board target = boardRepository.findById(boardCreate.id()).orElseThrow(() -> new IllegalStateException("테스트 게시판이 db에 존재하지 않습니다"));

		assertThat(boardCreate).extracting(BoardCreateResponse::id,
				BoardCreateResponse::name,
				BoardCreateResponse::description)
			.containsExactly(target.getId(), target.getName(), target.getDescription());
	}

	@Test
	void 임시게시판_자동생성_성공() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);

		//when
		boardService.create(createRequest);

		//then
		assertThat(boardRepository.existsByType(Board.Type.TEMPORARY)).isTrue();
	}

	@Test
	void 임시게시판_직접생성_실패() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("임시 게시판", "테스트 용", Board.Type.TEMPORARY);

		//when, then
		assertThatThrownBy(() -> boardService.create(createRequest))
			.isInstanceOf(BoardException.class)
			.hasMessageContaining(ExceptionCode.DUPLICATE_TEMP_BOARD.getMessage());

	}

	@Test
	void 게시판_id로_읽기_성공() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);
		BoardCreateResponse createResponse = boardService.create(createRequest);

		//when
		BoardReadResponse boardReadResponse = boardService.readById(createResponse.id());

		//then
		assertThat(boardReadResponse)
			.extracting(BoardReadResponse::id, BoardReadResponse::name, BoardReadResponse::description)
			.containsExactly(createResponse.id(), createResponse.name(), createResponse.description());
	}

	@Test
	void 게시판_id로_읽기_실패() {
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);
		boardService.create(createRequest);

		//when
		// 아무 아이디 입력

		//then
		assertThrows(IllegalArgumentException.class, () -> {
			boardService.readById(123123L);
		});
	}

	@Test
	void 게시판_이름으로_읽기_성공() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);
		BoardCreateResponse createResponse = boardService.create(createRequest);

		//when
		BoardReadResponse boardReadResponse = boardService.readByName("테스트 게시판");

		//then

		assertThat(boardReadResponse).extracting(BoardReadResponse::id)
			.isEqualTo(createResponse.id());
	}

	@Test
	void 게시판_이름으로_읽기_실패() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);
		boardService.create(createRequest);

		//when
		//then
		// assertThrows(IllegalArgumentException.class, () -> boardService.readByName("틀린 이름 입력"));

		assertThatThrownBy(() -> boardService.readByName("틀린 이름 입력"))
			.isInstanceOf(BoardException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_BOARD.getMessage());
	}

	@Test
	void 게시판_모두_읽기_성공() {
		BoardRequest.Create createRequest1 = new BoardRequest.Create("테스트 게시판1", "테스트 용", Board.Type.TEST);
		BoardRequest.Create createRequest2 = new BoardRequest.Create("테스트 게시판2", "테스트 용", Board.Type.FREE);
		BoardRequest.Create createRequest3 = new BoardRequest.Create("테스트 게시판3", "테스트 용", Board.Type.TEST);

		BoardCreateResponse createResponse1 = boardService.create(createRequest1);
		BoardCreateResponse createResponse2 = boardService.create(createRequest2);
		BoardCreateResponse createResponse3 = boardService.create(createRequest3);

		Board tempBoard = boardRepository.findByType(Board.Type.TEMPORARY).orElseThrow();
		Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
//		Page<BoardReadResponse> boardReadResponses = boardService.readAll(pageable);
//		assertThat(boardReadResponses)
//			.hasSize(4);
//		assertThat(boardReadResponses)
//			.extracting(BoardReadResponse::id)
//			.containsExactly(tempBoard.getId(), createResponse1.id(), createResponse2.id(), createResponse3.id());
	}

	@Test
	void 게시판_수정_성공() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);
		BoardCreateResponse createResponse = boardService.create(createRequest);

		//when
		BoardRequest.Update updateRequest = new BoardRequest.Update("(수정) 테스트 게시판", "수정 테스트");
		BoardUpdateResponse update = boardService.updateBoard(createResponse.id(), updateRequest);

		//then
		assertThat(update)
			.extracting(BoardUpdateResponse::name, BoardUpdateResponse::description)
			.containsExactly(updateRequest.name(), updateRequest.description());
	}

	@Test
	void 게시판_수정_실패() {
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);
		BoardCreateResponse createResponse = boardService.create(createRequest);

		BoardRequest.Update updateRequest = new BoardRequest.Update("(수정) 테스트 게시판", "수정 실패");

		// assertThrows(IllegalArgumentException.class, () -> boardService.updateBoard(1234L, updateRequest));

		assertThatThrownBy(() -> boardService.updateBoard(1234L, updateRequest))
			.isInstanceOf(BoardException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_BOARD.getMessage());
	}

	@Test
	void 임시게시판_수정_실패() {
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트 용", Board.Type.TEST);
		boardService.create(createRequest);

		Board temporaryBoard = boardRepository.findByType(Board.Type.TEMPORARY).orElseThrow();
		BoardRequest.Update updateRequest = new BoardRequest.Update("(수정) 임시 게시판", "수정 실패");

		assertThatThrownBy(() -> boardService.updateBoard(temporaryBoard.getId(), updateRequest))
			.isInstanceOf(BoardException.class)
			.hasMessageContaining(ExceptionCode.CANNOT_UPDATE_TEMP_BOARD.getMessage());
	}

	@Test
	void 일반게시판_삭제_성공() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트용", Board.Type.TEST);
		BoardCreateResponse testCreate = boardService.create(createRequest);

		//when
		boardService.delete(testCreate.id());

		//then
		assertThat(boardRepository.findById(testCreate.id())).isEmpty();
	}

	@Test
	void 일반게시판_삭제_실패() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트용", Board.Type.TEST);
		BoardCreateResponse testCreate = boardService.create(createRequest);

//		assertThrows(IllegalArgumentException.class, () -> boardService.delete(1234L));
	}

	@Test
	void 게시판_삭제후_게시글_임시게시판_이동_성공() {
		//given
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트 게시판", "테스트용", Board.Type.TEST);
		BoardCreateResponse createResponse = boardService.create(createRequest);

		Board testBoard = boardRepository.findById(createResponse.id()).orElseThrow(() -> new IllegalArgumentException("게시판 생성 x"));
		boardRepository.saveAndFlush(testBoard);
		Post testPost = new Post(testBoard, user, "테스트글", "테스트 글 입니다");
		//testBoard.posted(testPost);
		postRepository.saveAndFlush(testPost);

		//when
//		boardService.delete(deleteRequest);
		postRepository.flush();

		// extracting 객체에서 원하는 값만 추출 후 contains로 원래 게시판 id를 포함하나 확인
		//then
		Board tempBoard = boardRepository.findByType(Board.Type.TEMPORARY).orElseThrow();

		List<Post> posts = postRepository.findAllByBoardId(tempBoard.getId());
		assertThat(posts)
			.extracting(Post::getBoard)
			.extracting(Board::getId)
			.allMatch(id -> id.equals(tempBoard.getId()));

		// 원본 게시판에 있는 글들이 아직 남아 있나 한 번 더 확인
		List<Post> testBoardsPosts = postRepository.findAllByBoardId(testBoard.getId());
		assertThat(testBoardsPosts).isEmpty();
	}

	@Test
	void 게시판_삭제후_게시글_임시게시판_이동_성공2() {
		// 삭제할 일반 게시판 생성 및 저장
		Board boardToDelete = new Board("삭제 대상 게시판", "테스트 용", Board.Type.TEST);
		boardRepository.saveAndFlush(boardToDelete);

		var tempBoard = boardRepository.findByType(Board.Type.TEMPORARY).orElseThrow();

		// 게시글 생성 후 일반 게시판에 저장
		var posts = List.of(
			new Post(boardToDelete, user, "제목1", "내용1"),
			new Post(boardToDelete, user, "제목2", "내용2")
		);

		// 연관관계 단방향 전환
		//boardToDelete.posted(post1);
		//boardToDelete.posted(post2);
		postRepository.saveAllAndFlush(posts);

		// when
//		boardService.delete(request);

		// then 잠시 두기
//		List<Post> postsAfter = postRepository.findAll().stream()
//			.map(it -> it.getBoard().getId())
//			.distinct()
//			.count();

//		// 모든 게시글이 임시 게시판으로 이동했는지 확인
//		assertThat(postsAfter)
//			.extracting(post -> post.getBoard().getId())
//			.allMatch(id -> id.equals(tempBoard.getId()));
//			// allMatch를 사용해야할까? 고민해보기~
//		// 삭제한 게시판이 DB에 없는지 확인
//		assertThat(boardRepository.findById(boardToDelete.getId())).isEmpty();
	}

	@Test
	void 게시판_삭제후_게시글_임시게시판_이동_성공3() {
		// 삭제할 일반 게시판 생성 및 저장
		Board boardToDelete = new Board("삭제 대상 게시판", "테스트 용", Board.Type.TEST);
		Board board = boardRepository.saveAndFlush(boardToDelete);

		// 게시글 생성 후 일반 게시판에 저장
		Post post1 = new Post(board,
			user, "제목1", "내용1");
		Post post2 = new Post(board,
			user, "제목2", "내용2");

		postRepository.save(post1);
		postRepository.save(post2);
		postRepository.flush();

		// when: 게시판 삭제

//		boardService.delete(request);
		boardRepository.flush();

		Board tempBoard = boardRepository.findByType(Board.Type.TEMPORARY).orElseThrow();

		// then: 모든 게시글이 임시 게시판으로 이동했는지 확인
		List<Post> postsAfter = postRepository.findAll();
		assertThat(postsAfter)
			.extracting(post -> post.getBoard().getId())
			.allMatch(id -> id.equals(tempBoard.getId()));

		// 삭제한 게시판이 DB에 없는지 확인
		assertThat(boardRepository.findById(boardToDelete.getId())).isEmpty();
	}

	@Test
	void 임시게시판_삭제_실패() {
		BoardRequest.Create createRequest = new BoardRequest.Create("테스트용", "테스트", Board.Type.TEST);
		boardService.create(createRequest);
		Board tempBoard = boardRepository.findByType(Board.Type.TEMPORARY).orElseThrow(() -> new IllegalStateException("테스트 실패"));

//		assertThrows(IllegalStateException.class, () -> boardService.delete(tempBoard.getId()));
	}
}
