package me.boardApp.comment;

import jakarta.transaction.Transactional;
import me.boardApp.domain.comment.Comment;
import me.boardApp.domain.comment.dto.CommentCreateResponse;
import me.boardApp.domain.comment.dto.CommentReadResponse;
import me.boardApp.domain.comment.CommentRepository;
import me.boardApp.domain.comment.dto.CommentResponse;
import me.boardApp.domain.comment.service.CommentService;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.BoardRepository;
import me.boardApp.global.dto.request.CommentRequest;
import me.boardApp.global.exception.ApiException;
import me.boardApp.global.exception.CommentException;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.post.PostRepository;
import me.boardApp.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class CommentServiceTest {

	@Autowired
	private CommentService commentService;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private CommentRepository commentRepository;
	private Long testBoardId;
	private Long testPostId;

	User user;

	@BeforeEach
	void 유저_생성(){
		user = new User("최승연", "얍얍", "1234", "csy03178@naver.com");
	}

	@BeforeEach
	void 게시판_게시글_생성(){
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		testBoardId = testBoard.getId();

		Post testPost = new Post(testBoard, user, "테스트 글", "본문");
		postRepository.save(testPost);
		testPostId = testPost.getId();
	}

	@Test
	void 댓글_생성_성공(){
		//given
		CommentRequest.Create request = new CommentRequest.Create(user.getNickname(), "댓글");

		//when
		CommentCreateResponse commentCreateResponse = commentService.create(testPostId, request);

		//then
		assertThat(commentRepository.findById(commentCreateResponse.id()))
			.get()
			.extracting(Comment::getUser,  Comment::getComment)
			.containsExactly(user, "댓글");

	}

	@Test
	void 댓글_생성_실패(){
		//given
		CommentRequest.Create request = new CommentRequest.Create(user.getNickname(), "댓글");

		//when, then
		assertThatThrownBy(() -> commentService.create(1234L, request))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_POST.getMessage());
	}

	@Test
	void 댓글_댓글쓴사람_게시글Id로_읽기_성공(){
		//given
		CommentRequest.Create request1 = new CommentRequest.Create(user.getNickname(), "댓글1");
		CommentRequest.Create request2 = new CommentRequest.Create(user.getNickname(), "댓글2");
		commentService.create(testPostId, request1);
		commentService.create(testPostId, request2);

		//when,then
		assertThat(commentService.readByWriterAndPostId("얍얍",testPostId))
			.extracting(CommentReadResponse::comment)
			.containsExactly("댓글1", "댓글2");
	}
	@Test
	void 댓글_id로_읽기_성공(){
		//given
		CommentRequest.Create request = new CommentRequest.Create(user.getNickname(), "댓글");
		CommentCreateResponse response = commentService.create(testPostId, request);

		//when, then
		assertThat(commentService.readByCommentId(response.id()))
			.extracting(CommentReadResponse::id)
			.isEqualTo(response.id());
	}

	@Test
	void 댓글_id로_읽기_실패(){
		//given
		CommentRequest.Create request = new CommentRequest.Create(user.getNickname(),"댓글");
		commentService.create(testPostId, request);

		//when, then
		assertThatThrownBy(() -> commentService.readByCommentId(1234L))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_COMMENT.getMessage());
	}

	@Test
	void 댓글_게시글id_로_읽기_성공(){
		//given
		CommentRequest.Create createRequest1 = new CommentRequest.Create(user.getNickname(),"댓글1");
		CommentCreateResponse commentCreateResponse1 = commentService.create(testPostId, createRequest1);

		CommentRequest.Create createRequest2 = new CommentRequest.Create(user.getNickname(),"댓글2");
		CommentCreateResponse commentCreateResponse2 =commentService.create(testPostId, createRequest2);

		//when, then
		assertThat(commentService.readAllByPostId(testPostId))
			.extracting(CommentReadResponse::id)
			.containsExactly(commentCreateResponse1.id(),commentCreateResponse2.id());
	}

	@Test
	void 댓글_게시글id_로_읽기_실패(){
		//given
		CommentRequest.Create createRequest1 = new CommentRequest.Create(user.getNickname(),"댓글1");
		commentService.create(testPostId, createRequest1);

		CommentRequest.Create createRequest2 = new CommentRequest.Create(user.getNickname(),"댓글2");
		commentService.create(testPostId, createRequest2);

		//when, then
		assertThatThrownBy(() -> commentService.readAllByPostId(1234L))
		.isInstanceOf(ApiException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_POST.getMessage());
	}

	@Test
	void 댓글_수정_성공(){
		//given
		CommentRequest.Create createRequest = new CommentRequest.Create(user.getNickname(), "댓글");
		CommentResponse.Read createResponse = commentService.create(testPostId, createRequest);

		//when
		CommentRequest.Update updateRequest = new CommentRequest.Update("얍얍", "1234", "(수정) 댓글");
		commentService.updateComment(createResponse.id(), updateRequest);

		//then
		assertThat(commentService.readByCommentId(createResponse.id()))
			.extracting(CommentResponse.Read::comment)
			.isEqualTo("(수정) 댓글");
	}

	@Test
	void 댓글_수정_실패(){
		//given
		CommentRequest.Create createRequest = new CommentRequest.Create(user.getNickname(), "댓글");
		CommentCreateResponse createResponse = commentService.create(testPostId, createRequest);

		//when
		CommentRequest.Update updateRequest1 = new CommentRequest.Update("얍얍", "1234", "(수정) 댓글");
		CommentRequest.Update updateRequest2 = new CommentRequest.Update("얍얍x", "1234", "(수정) 댓글");
		CommentRequest.Update updateRequest3 = new CommentRequest.Update("얍얍", "1111", "(수정) 댓글");

		//then
		// 댓글 id 틀림
		assertThatThrownBy(()->commentService.updateComment(1234L, updateRequest1))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_COMMENT.getMessage());

		// 댓글 작성자 틀림
		assertThatThrownBy(()->commentService.updateComment(createResponse.id(), updateRequest2))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_NICKNAME.getMessage());

		// 댓글 작성자 비밀번호 틀림
		assertThatThrownBy(()->commentService.updateComment(createResponse.id(), updateRequest3))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.INVALID_PASSWORD.getMessage());
	}

	@Test
	void 댓글_id로_삭제_성공(){
		//given
		CommentRequest.Create createRequest = new CommentRequest.Create(user.getNickname(), "댓글");
		CommentCreateResponse createResponse = commentService.create(testPostId, createRequest);

		CommentRequest.Delete deleteRequest = new CommentRequest.Delete("얍얍", "1234");
		commentRepository.flush();

		//when
		commentService.deleteByCommentId(createResponse.id(),deleteRequest);
		commentRepository.flush();

		//then
		assertThat(commentRepository.findById(createResponse.id())).isEmpty();

		assertThatThrownBy(() -> commentService.readByCommentId(createResponse.id()))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_COMMENT.getMessage());
	}

	@Test
	void 댓글_id로_삭제_실패(){
		//given
		CommentRequest.Create createRequest = new CommentRequest.Create(user.getNickname(), "댓글");
		CommentCreateResponse createResponse = commentService.create(testPostId, createRequest);

		CommentRequest.Delete deleteRequest1 = new CommentRequest.Delete("얍얍", "1234");
		CommentRequest.Delete deleteRequest2 = new CommentRequest.Delete("닉네임 틀림", "1234");
		CommentRequest.Delete deleteRequest3 = new CommentRequest.Delete("얍얍", "1111");
		//when
		//then
		// 댓글 아이디 틀림
		assertThatThrownBy(() -> commentService.deleteByCommentId(1234L, deleteRequest1))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_COMMENT.getMessage());

		// 작성자 이름 틀림
		assertThatThrownBy(() -> commentService.deleteByCommentId(createResponse.id(), deleteRequest2))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_NICKNAME.getMessage());

		// 댓글 비밀번호 틀림
		assertThatThrownBy(() -> commentService.deleteByCommentId(createResponse.id(), deleteRequest3))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.INVALID_PASSWORD.getMessage());
	}

	@Test
	void 작성자_이름으로_모든_댓글_삭제_성공(){
		//given
		CommentRequest.Create createRequest1 = new CommentRequest.Create(user.getNickname(), "댓글1");
		commentService.create(testPostId, createRequest1);

		CommentRequest.Create createRequest2 = new CommentRequest.Create(user.getNickname(), "댓글2");
		commentService.create(testPostId, createRequest2);
		// delete후 findAll() 처럼 쿼리를 강제로 날리는 경우
		// 아직 영속성 컨텍스트에만 존재하고 db에 반영안됐을 수 있음
		// 또는 다른 엔티티가 연관관계 때문에 아직 영속화되지 않은 객체를 참조하면 flush가 필요
		commentRepository.flush();
		//when
		CommentRequest.Delete deleteRequest = new CommentRequest.Delete("얍얍", "1234");
		commentService.deleteAllByWriter(deleteRequest);

		//then
		assertThat(commentRepository.findAll()).hasSize(0);
		Post post = postRepository.findById(testPostId).orElseThrow();
		assertThat(post.getComments()).isEmpty();
	}

	@Test
	void 작성자_이름으로_모든_댓글_삭제_실패(){}

	@Test
	void 게시글_삭제시_댓글_삭제_성공(){
		//given
		CommentRequest.Create createRequest = new CommentRequest.Create(user.getNickname(), "댓글");
		CommentCreateResponse createResponse = commentService.create(testPostId, createRequest);

		//when
		postRepository.delete(postRepository.findById(testPostId).orElseThrow());

		//then
		assertThatThrownBy(()-> commentService.readByCommentId(createResponse.id()))
			.isInstanceOf(CommentException.class)
			.hasMessageContaining(ExceptionCode.NOT_FOUND_COMMENT.getMessage());
	}
}
