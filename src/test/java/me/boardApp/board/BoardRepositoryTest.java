package me.boardApp.board;

import jakarta.transaction.Transactional;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.BoardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class BoardRepositoryTest {

	@Autowired
	private BoardRepository boardRepository;

	@Test
	void 게시판_저장_성공(){
		//given
		Board savedBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		//when
		boardRepository.save(savedBoard);
		//then
		assertThat(boardRepository.findById(savedBoard.getId())).isPresent().contains(savedBoard);
		assertThat(boardRepository.findById(savedBoard.getId()).get()).isEqualTo(savedBoard);
	}

	@Test
	void 게시판아이디_자동생성_성공(){
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		Board savedBoard = boardRepository.findById(testBoard.getId()).orElseThrow(()->new RuntimeException("오류"));
		assertThat(savedBoard.getId()).isNotNull();
	}

	@Test
	void 게시판_조회_성공(){
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		Board savedBoard = boardRepository.findById(testBoard.getId()).orElseThrow(()->new IllegalStateException("존재하지 않음"));
		assertThat(savedBoard).isEqualTo(testBoard);
	}

	@Test
	void 게시판_수정_성공(){
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		testBoard.update("(수정) 테스트 게시판", "본문 수정 완료");

		Board updateBoard = boardRepository.findById(testBoard.getId()).orElseThrow();
		assertThat(updateBoard.getName()).isEqualTo("(수정) 테스트 게시판");
		assertThat(updateBoard.getDescription()).isEqualTo("본문 수정 완료");
	}

	@Test
	void 게시판_삭제_성공(){
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		boardRepository.delete(testBoard);
		assertThat(boardRepository.findById(testBoard.getId())).isEmpty();
	}

	@Test
	void 게시판_아이디로삭제_성공(){
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		boardRepository.deleteById(testBoard.getId());
		assertThat(boardRepository.findById(testBoard.getId())).isEmpty();
	}

	// 이 테스트는 서비스 로직을 검사하는 거니까 서비스 테스트로 옮기자
//	@Test
//	void 임시게시판_삭제_실패(){
//		Board temp = new Board("임시 게시판", "임시 게시판", Board.Type.TEMPORARY);
//		boardRepository.save(temp);
//
//		assertThrows(IllegalStateException.class, () -> boardService.delete(temp.getId()));
//	}
}
