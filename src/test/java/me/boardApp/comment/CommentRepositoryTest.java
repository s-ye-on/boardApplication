package me.boardApp.comment;

import jakarta.transaction.Transactional;
import me.boardApp.domain.comment.Comment;
import me.boardApp.domain.comment.CommentRepository;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.BoardRepository;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.post.PostRepository;
import me.boardApp.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CommentRepositoryTest {
	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private PostRepository postRepository;

	private Long testPostId;

	User user;

	@BeforeEach
	void 유저_생성(){
		user = new User("최승연", "얍얍", "1234", "csy03178@naver.com");
	}

	@BeforeEach
	public void 게시판_게시글_생성() {
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);

		Post testPost = new Post(testBoard,user, "테스트 글", "본문");
		postRepository.save(testPost);
		// 연관관계 단방향 전환
		//testBoard.posted(testPost);
		testPostId = testPost.getId();
	}
	@Test
	void 댓글_생성_성공(){
		Post testPost = postRepository.findById(testPostId).orElseThrow();
		Comment testComment = new Comment(testPost, user,"테스트 댓글");
		commentRepository.save(testComment);

	}
	@Test
	void 댓글_저장_성공(){
		Post testPost = postRepository.findById(testPostId).orElseThrow();
		Comment testComment = new Comment(testPost, user,"테스트 댓글");
		commentRepository.save(testComment);

		// Comment 객체 끼리 비교
		assertThat(commentRepository.findById(testComment.getId()).orElseThrow())
			.isEqualTo(testComment);

		// Optional 안에 testComment가 존재하는지 검증
		assertThat(commentRepository.findById(testComment.getId()))
			.contains(testComment);
	}
	@Test
	void 댓글_읽기_성공(){
		Post testPost = postRepository.findById(testPostId).orElseThrow();

		Comment testComment = new Comment(testPost, user, "테스트 댓글");
		commentRepository.save(testComment);

		assertThat(commentRepository.findById(testComment.getId()).orElseThrow())
		.isEqualTo(testComment);

	}
	@Test
	void 댓글_작성자_게시글Id로_읽기_성공(){
		//given
		Post testPost = postRepository.findById(testPostId).orElseThrow();

		Comment testComment1 = new Comment(testPost, user, "테스트 댓글1");
		commentRepository.save(testComment1);

		Comment testComment2 = new Comment(testPost, user, "테스트 댓글2");
		commentRepository.save(testComment2);

		//when
		List<Comment> results = commentRepository.findAllByUserNicknameAndPostId("최승연", testPost.getId());

		//then
		assertThat(results.size()).isEqualTo(2);
		assertThat(results.get(0)).isEqualTo(testComment1);
		assertThat(results.get(1)).isEqualTo(testComment2);
	}
	@Test
	void 댓글_게시글Id로_모두_읽기_성공(){
		Post testPost = postRepository.findById(testPostId).orElseThrow();

		Comment testComment1 = new Comment(testPost, user, "테스트 댓글");
		commentRepository.save(testComment1);

		Comment testComment2 = new Comment(testPost, user, "테스트 댓글");
		commentRepository.save(testComment2);

		List<Comment> results = commentRepository.findAllByPostId(testPost.getId());

		assertThat(results.size()).isEqualTo(2);
		assertThat(results.get(0)).isEqualTo(testComment1);
		assertThat(results.get(1)).isEqualTo(testComment2);
	}
	@Test
	void 댓글_수정_성공(){
		Post testPost = postRepository.findById(testPostId).orElseThrow();
		Comment testComment = new Comment(testPost, user, "테스트 댓글");
		commentRepository.save(testComment);

		testComment.update("(수정) 댓글 수정");
		Comment foundComment = commentRepository.findById(testComment.getId()).orElseThrow();

		assertThat(foundComment.getComment()).isEqualTo("(수정) 댓글 수정");

	}
	@Test
	void 댓글_삭제_성공(){
		//given
		Post testPost = postRepository.findById(testPostId).orElseThrow();

		Comment testComment1 = new Comment(testPost, user, "테스트 댓글1");
		commentRepository.save(testComment1);

		Comment testComment2 = new Comment(testPost, user, "테스트 댓글2");
		commentRepository.save(testComment2);

		//when
		commentRepository.delete(testComment1);
		commentRepository.delete(testComment2);

		//then
		assertThat(commentRepository.findById(testComment1.getId())).isEmpty();
		assertThat(commentRepository.findById(testComment2.getId()).isPresent()).isFalse();
	}
}
