package me.boardApp.post;

import jakarta.transaction.Transactional;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.BoardRepository;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.post.dto.PostResponse;
import me.boardApp.global.dto.request.PostRequest;
import me.boardApp.domain.post.PostRepository;
import me.boardApp.domain.post.service.PostService;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.exception.BoardException;
import me.boardApp.global.exception.PostException;
import me.boardApp.global.exception.UserException;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class PostServiceTest {
	@Autowired
	private PostService postService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	Long testBoardId;
	User user;

	@BeforeEach
	void 게시판_유저_생성() {
		// 게시판 생성
		Board testBoard = new Board("테스트 게시판", "테스트용", Board.Type.TEST);
		boardRepository.save(testBoard);
		testBoardId = testBoard.getId();

		// 유저 생성
		user = new User("최승연", "얍얍", passwordEncoder.encode("1234"), "csy03178@naver.com");
		userRepository.save(user);
	}

	@Test
	void 게시글_생성_성공() {
		//given
		PostRequest.Create request = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		Pageable pageable = PageRequest.of(0, 30, Sort.by("createdDate").descending());

		//when
		PostResponse.Create postCreateResponse = postService.create(request, user.getId());

		//then
		Long postId = postCreateResponse.id();
		Post foundPost = postRepository.findById(postId).orElseThrow();

		assertThat(foundPost.getTitle()).isEqualTo("테스트 글");
		assertThat(foundPost.getText()).isEqualTo("본문");
		assertThat(foundPost.getUser().getNickname()).isEqualTo("얍얍");

		//board에 게시글 포함되어 있는지 확인
		// 단방향으로 전환했으므로 Board에서 바로 게시글을 꺼낼 수 없음
		// PostRepository를 통해 Board 기준으로 조회하도록 바꿔야함
		Page<Post> boardPosts = postRepository.findAllByBoardId(testBoardId, pageable);
		assertThat(boardPosts)
			.extracting(Post::getId)
			.containsExactly(postId);

		List<PostResponse.Read> boardPosts2 = postService.readAllByBoardId(testBoardId, pageable).getContent();
		assertThat(boardPosts2)
			.extracting(PostResponse.Read::title)
			.containsExactly("테스트 글");
	}

	@Test
	void 게시글_저장_실패() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(1234L, "테스트 글", "본문");

		//when

		//then
		assertThatThrownBy(() -> postService.create(postCreateRequest, user.getId()))
			.isInstanceOf(BoardException.class)
			.hasMessageContaining("게시판이 존재하지 않습니다");
	}

	@Test
	void 게시글_저장시_게시판에_저장_확인() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		Pageable pageable = PageRequest.of(0, 30, Sort.by("createdDate").descending());

		//when
		PostResponse.Create postCreateResponse = postService.create(postCreateRequest, user.getId());

		//then
		List<Post> boardPosts = postRepository.findAllByBoardId(testBoardId, pageable).getContent();

		assertThat(boardPosts)
			.extracting(Post::getId)
			.contains(postCreateResponse.id());
	}

	@Test
	void 게시글_Id로_읽기_성공() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		PostResponse.Create postCreateResponse = postService.create(postCreateRequest, user.getId());
		String boardName = boardRepository.findById(testBoardId).orElseThrow().getName();

		//when
		PostResponse.Read postReadResponse = postService.readByPostId(postCreateResponse.id());

		//then
		assertThat(postReadResponse)
			.extracting(PostResponse.Read::title, PostResponse.Read::boardName, PostResponse.Read::writer)
			.containsExactly("테스트 글", boardName, postCreateResponse.writer());
	}

	@Test
	void 게시글_Id로_읽기_실패() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		postService.create(postCreateRequest, user.getId());

		//when

		//then
		assertThatThrownBy(() -> postService.readByPostId(1234L))
			.isInstanceOf(PostException.class)
			.hasMessageContaining("게시글이 존재하지 않습니다");
	}

	@Test
	void 게시글_조회시_조회수_증가_성공() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		PostResponse.Create postCreateResponse = postService.create(postCreateRequest, user.getId());

		//when
		postService.readByPostId(postCreateResponse.id());
		postService.readByPostId(postCreateResponse.id());

		//then
		assertThat(postService.readByPostId(postCreateResponse.id()))
			.extracting(PostResponse.Read::views)
			.isEqualTo(3L);
	}

	@Test
	void 게시글_제목으로_읽기_성공() {
		//given
		PostRequest.Create postCreateRequest1 = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		PostRequest.Create postCreateRequest2 = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		postService.create(postCreateRequest1, user.getId());
		postService.create(postCreateRequest2, user.getId());

		//when, then
		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		assertThat(postService.readByTitle("테스트 글", pageable).getContent()).hasSize(2);
		assertThat(postService.readByTitle("테스트 글", pageable).getContent())
			.extracting(PostResponse.Read::title)
			.contains("테스트 글");
	}

	@Test
	void 게시글_제목으로_읽기_실패() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		postService.create(postCreateRequest, user.getId());

		//when, then
		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		assertThat(postService.readByTitle("잘못된 입력", pageable).getContent()).hasSize(0);
		assertThat(postService.readByTitle("잘못된 입력", pageable).getContent()).isEmpty();
	}

	@Test
	void 게시글_게시판Id로_읽기_성공() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		postService.create(postCreateRequest, user.getId());

		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		assertThat(postService.readAllByBoardId(testBoardId, pageable).getContent()).hasSize(1);
		assertThat(postService.readAllByBoardId(testBoardId, pageable).getContent())
			.extracting(PostResponse.Read::title, PostResponse.Read::boardName)
			.containsExactly(Tuple.tuple("테스트 글", "테스트 게시판"));
		// List를 반환하는 assertThat이라 그럼 각 원소마다 튜플을 만들어 리스트로 반환함
		// .extracting(메서드1, 메서드2) -> tuple 리스트 반환
	}

	@Test
	void 게시글_게시판Id로_읽기_실패() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		postService.create(postCreateRequest, user.getId());

		//when, then
		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		assertThatThrownBy(() -> postService.readAllByBoardId(1234L, pageable))
			.isInstanceOf(BoardException.class)
			.hasMessageContaining("게시판이 존재하지 않습니다");
	}

	@Test
	void 게시글_수정_성공() {
		//given
		PostRequest.Create request = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		PostResponse.Create createResponse = postService.create(request, user.getId());
		PostRequest.Update updateRequest = new PostRequest.Update("1234", "(수정) 테스트 글", "(수정) 본문");

		//when
		PostResponse.Update updateResponse = postService.update(createResponse.id(), updateRequest, user.getId());

		//then
		assertThat(updateResponse)
			.extracting("title", "text")
			.containsExactly("(수정) 테스트 글", "(수정) 본문");
	}

	@Test
	void 게시글_수정_실패() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		PostResponse.Create createResponse = postService.create(postCreateRequest, user.getId());

		PostRequest.Update updateRequest1 = new PostRequest.Update("1234", "(수정) 테스트 글", "(수정) 본문");
		PostRequest.Update updateRequest2 = new PostRequest.Update("1111", "(수정) 테스트 글", "(수정) 본문");

		//when
		//then
		assertThatThrownBy(() -> postService.update(1234L, updateRequest1, user.getId()))
			.isInstanceOf(PostException.class)
			.hasMessageContaining("게시글이 존재하지 않습니다");

		assertThatThrownBy(() -> postService.update(createResponse.id(), updateRequest2, user.getId()))
			.isInstanceOf(UserException.class)
			.hasMessageContaining("비밀번호가 일치하지 않습니다");

	}

	@Test
	void 게시글_삭제_성공() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		PostResponse.Create postCreateResponse = postService.create(postCreateRequest, user.getId());
		PostRequest.Delete postDeleteRequest = new PostRequest.Delete("1234");

		Pageable pageable = PageRequest.of(0, 30, Sort.by("createdDate").descending());

		//when
		postService.delete(postCreateResponse.id(), postDeleteRequest, user.getId());

		//then
		List<Post> boardPosts = postRepository.findAllByBoardId(testBoardId, pageable).getContent();

		assertThat(boardPosts)
			.extracting(Post::getId)
			.doesNotContain(postCreateResponse.id());
	}

	@Test
	void 게시글_삭제_실패() {
		//given
		PostRequest.Create postCreateRequest = new PostRequest.Create(testBoardId, "테스트 글", "본문");
		PostResponse.Create postCreateResponse = postService.create(postCreateRequest, user.getId());

		PostRequest.Delete postDeleteRequest1 = new PostRequest.Delete("1234");
		PostRequest.Delete postDeleteRequest2 = new PostRequest.Delete("1111");

		//when
		assertThatThrownBy(() -> postService.delete(1234L, postDeleteRequest1, user.getId()))
			.isInstanceOf(PostException.class)
			.hasMessageContaining("게시글이 존재하지 않습니다");

		assertThatThrownBy(() -> postService.delete(postCreateResponse.id(), postDeleteRequest2, user.getId()))
			.isInstanceOf(UserException.class)
			.hasMessageContaining("비밀번호가 일치하지 않습니다");
	}
}
