package me.boardApp.user;

import jakarta.transaction.Transactional;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.BoardRepository;
import me.boardApp.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
public class UserRepositoryTest {
	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private UserRepository userRepository;

	private Long testBoardId;
	private Board testBoard;
	@BeforeEach
	void 게시판_생성(){
		testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		testBoardId = testBoard.getId();
	}

	@Test
	void 유저_생성_성공(){

	}
}
