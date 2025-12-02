package me.boardApp.post;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.BoardRepository;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.post.PostRepository;
import me.boardApp.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class PostRepositoryTest {
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private BoardRepository boardRepository;
	private Long testBoardId;
	private User user;


	@PersistenceContext
	private EntityManager em; // 영속성 컨텍스트 제어용

	@BeforeEach
	void 게시판_생성(){
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		testBoardId = testBoard.getId();
	}

	@BeforeEach
	void 유저_생성(){
		user = new User("최승연", "얍얍", "cs123", "csy03178@naver.com");
	}
	@Test
	void 게시글_생성_성공(){
		// given
		Board testBoard = boardRepository.findById(testBoardId).orElse(null);
		Post testPost = new Post(testBoard, user, "테스트글", "테스트글 본문");

		//when
		postRepository.save(testPost);
		// 연관관계 단방향 전환
		//testBoard.posted(testPost);

		//then
		// 게시글 생성 확인
		Post foundPost = postRepository.findById(testPost.getId()).orElseThrow();
		assertThat(foundPost).isEqualTo(testPost);
	}

	@Test
	@Disabled("수정 후 다시 테스트")
	void 게시글_저장시_게시판에_저장_성공(){
		// given
		Board testBoard = boardRepository.findById(testBoardId).orElse(null);
		Post testPost = new Post(testBoard, user, "테스트글", "테스트글 본문");

		//when
		postRepository.save(testPost);
		// 연관관계 단방향 전환
		//testBoard.posted(testPost);

		//then
		// 게시글 게시판에 저장됐는지 확인
		List<Post> posts = postRepository.findAllByBoardId(testBoard.getId());
		assertThat(posts).hasSize(1);
		assertThat(posts.get(0).getId()).isEqualTo(testPost.getId());

	}

	@Test
	void 게시글_Id로_읽기_성공(){
		//given
		Board testBoard = boardRepository.findById(testBoardId).orElse(null);
		Post testPost = new Post(testBoard, user, "테스트글", "본문");
		postRepository.save(testPost);
		// 연관관계 단방향 전환
		//testBoard.posted(testPost);

		//when
		Post foundPost = postRepository.findById(testPost.getId()).orElseThrow();

		//then
		assertThat(testPost.getId()).isEqualTo(foundPost.getId());

	}
	@Test
	void 게시글_제목으로_읽기_성공(){
		//given
		Board testBoard = boardRepository.findById(testBoardId).orElse(null);
		Post testPost1 = new Post(testBoard, user, "테스트글", "본문");
		Post testPost2 = new Post(testBoard, user, "테스트글", "본문");
		postRepository.save(testPost1);
		//testBoard.posted(testPost1);
		postRepository.save(testPost2);
		//testBoard.posted(testPost2);

		//when
		List<Post> foundPosts = postRepository.findAllByTitle("테스트글");

		//then
		assertThat(foundPosts).hasSize(2);
		assertThat(foundPosts.get(0).getId()).isEqualTo(testPost1.getId());
		assertThat(foundPosts.get(1).getId()).isEqualTo(testPost2.getId());
	}
	@Test
	void 게시글_게시판Id로_읽기_성공(){
		//given
		Board testBoard = boardRepository.findById(testBoardId).orElse(null);
		Post testPost1 = new Post(testBoard, user, "테스트글1", "본문");
		Post testPost2 = new Post(testBoard, user, "테스트글2", "본문");
		postRepository.save(testPost1);
		// 연관관계 단방향 전환
		//testBoard.posted(testPost1);
		postRepository.save(testPost2);
		//testBoard.posted(testPost2);

		//when
		List<Post> foundPosts = postRepository.findAllByBoardId(testBoard.getId());

		//then
		assertThat(foundPosts).hasSize(2);
		assertThat(foundPosts.get(0).getId()).isEqualTo(testPost1.getId());
		assertThat(foundPosts.get(1).getId()).isEqualTo(testPost2.getId());
	}
	@Test
	void 게시글_수정_성공(){
		//given
		Board testBoard = boardRepository.findById(testBoardId).orElse(null);
		Post testPost = new Post(testBoard, user, "테스트글", "본문");
		postRepository.save(testPost);
		//testBoard.posted(testPost);

		//when
		testPost.update("(수정) 테스트글", "수정");
		Post foundPost = postRepository.findById(testPost.getId()).orElseThrow();

		//then
		assertThat(foundPost.getTitle()).isEqualTo("(수정) 테스트글");
		assertThat(foundPost.getText()).isEqualTo("수정");

	}
	@Test
	void 게시글_삭제_성공(){
		//given
		Board testBoard = boardRepository.findById(testBoardId).orElse(null);
		Post testPost = new Post(testBoard, user, "테스트글", "본문");
		postRepository.save(testPost);
		//testBoard.posted(testPost);

		//when
		// 연관관계 단방향 전환
		//testBoard.getPosts().remove(testPost);
		postRepository.delete(testPost);
		postRepository.flush(); // delete 쿼리를 즉시 DB에 반영
		em.clear(); // 이미 로드된 Board와 Post 객체를 초기화해서 DB상태와 동기화
		// orphanRemoval
		// 컬렉션에서 제거되면 자동으로 db에서 삭제 (컬렉션: 리스트에서 삭제되어야함)
		// 하지만 delete(post) 시 영속성 컨텍스트 컬렉션에서는 여전히 남아 있을 수 있음 -> flush + clear 필요

		Board foundBoard = boardRepository.findById(testBoard.getId()).orElseThrow();

		//then
		//게시글 삭제
		assertThat(postRepository.existsById(testPost.getId())).isFalse();
		//게시판 여전히 존재
		assertThat(boardRepository.existsById(testBoard.getId())).isTrue();

		// 게시판에 게시글 존재하는지 확인
		// DB에서 다시 불러오기
		/*
		JPA에서 이미 로드된 컬렉션은 DB 삭제와 자동 동기화되지 않는다
		테스트에서는 DB에서 다시 조회한 객체로 검증하는 게 안전함
		 */
		List<Post> foundPosts = postRepository.findAllByBoardId(testBoard.getId());
		assertThat(foundPosts).hasSize(0);

	}
}
