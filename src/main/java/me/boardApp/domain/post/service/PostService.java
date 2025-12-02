package me.boardApp.domain.post.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.notice.Notice;
import me.boardApp.domain.post.dto.PostResponse;
import me.boardApp.global.CommonService;
import me.boardApp.global.dto.request.PostRequest;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.post.PostRepository;
import me.boardApp.domain.user.User;
import me.boardApp.global.exception.NoticeException;
import me.boardApp.global.exception.PostException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;
	private final PasswordEncoder passwordEncoder;
	private final CommonService commonService;

	// Create
	public PostResponse.Create create(PostRequest.Create request) {
		Board board = commonService.getBoardById(request.boardId());

		User writer =  commonService.getUserByNickname(request.nickname());

		Post post = new Post(
			board,
			writer,
			request.title(),
			request.text()
		);
			// 단방향 전환
//		board.posted(post);

		postRepository.save(post);

		return new PostResponse.Create(
			post.getId(),
			board.getName(),
			post.getTitle(),
			writer.getNickname()
		);
	}

	/// todo: 관리자인지 확인을 지금 commonService에서 하고 있지만 AuthService로 옮기면 어떨까 생각
	public PostResponse.Create createNotice(PostRequest.Create request) {
		Board board = commonService.getBoardById(request.boardId());
		User writer =  commonService.getUserByNickname(request.nickname());

		commonService.validateAdmin(writer);

		Notice notice = new Notice(
			board,
			writer,
			request.title(),
			request.text()
		);

		postRepository.save(notice);

		return new PostResponse.Create(
			notice.getId(),
			board.getName(),
			notice.getTitle(),
			writer.getNickname()
		);
	}

	// Read
	// 일반적인 게시판을 생각했기에 오프셋 방법이 적절할 것 같음
	// 인스타같은 그런 커뮤니티였다면 커서 기반도 좋음
	// 이럴 때 dip로 UseCase 인터페이스 두고 각각 구현(?)

	// 오프셋 기반
	public Page<PostResponse.Read> readALl(Pageable pageable) {
//		return postRepository.findAll().stream()
//			.map(this::toResponse)
//			.toList();
		// 정렬 조건을 직접 지정하지 않았기에 요청이 들어올 때의 Pageable의 파라미터에 따라 달라짐
		return postRepository.findAll(pageable)
			.map(this::toResponse);
	}

	// 커서 기반
	public List<PostResponse.Read> readAllPostsCursor(Long lastPostId, Pageable pageable) {
		Page<Post> posts;

		if (lastPostId == null) {
			// 처음 조회 시 : 최신 글부터 pageable.size만큼 가져오기
			posts = postRepository.findTop10ByOrderByIdDesc(pageable);
		} else {
			// lastPostId보다 작은 ID만 대상으로 pageable.size 만큼 가져오기
			posts = postRepository.findByIdLessThanOrderByIdDesc(lastPostId, pageable);
		}

		return posts.stream()
			.map(this::toResponse)
			.toList();
	}

	public PostResponse.Read readByPostId(Long postId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new PostException(ExceptionCode.NOT_FOUND_POST));

		post.viewed();

		return toResponse(post);
	}

	public List<PostResponse.Read> readAllNoticeByBoardId(Long boardId){
		return postRepository.findALlNoticeByBoardId(boardId)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	// 게시판의 글을 조회하는 것 -> 게시판의 책임이 맞지만
	// 실제 글 조회는 Post Entity와 PostRepository에서 일어나서 여기다 두는게 맞음
	public Page<PostResponse.Read> readAllByBoardId(Long boardId,  Pageable pageable) {
		//게시판 존재 여부 확인
		commonService.getBoardById(boardId);

		return postRepository.findAllByBoardId(boardId, pageable)
			.map(this::toResponse);
	}

	public Page<PostResponse.Read> readByTitle(String title, Pageable pageable) {
		// 그냥 빈 리스트를 반환하게 해서 없구나 하는걸 알게 하고 싶음
		return postRepository.findAllByTitle(title, pageable)
			.map(this::toResponse);
	}

	public Page<PostResponse.Read> readByWriter(String nickname, Pageable pageable) {
		User user = commonService.getUserByNickname(nickname);
		return postRepository.findAllByUserNickname(user.getNickname(), pageable)
			.map(this::toResponse);
	}

	private PostResponse.Read toResponse(Post post) {
		return new PostResponse.Read(
			post.getTitle(),
			post.getBoard().getName(),
			post.getUser().getNickname(),
			post.getViews()
		);
	}

	public Post getEntityByPostId(Long postId) {
		return postRepository.findById(postId)
			.orElseThrow(()-> new PostException(ExceptionCode.NOT_FOUND_POST));
	}

	// Update
	// 제목 수정, 본문 수정
	public PostResponse.Update updatePost(Long id, PostRequest.Update request) {
		// postRepository.findById(id) -> Spring Data JPA가 제공하는 메서드인데
		// 리턴타입이 Optional<T>로 되어 있음
		// 항상 Optional로 반환
		Post post = getEntityByPostId(id);
		// 1. Objects.equals
		// 2. 검증이 끝난 값으로 equals를 호출하기
		// 3. Bean Validation
//		post.validateWriterAndPassword(postUpdateRequest.writer(), postUpdateRequest.password(),  passwordEncoder);
		User writer = post.getUser();
		writer.validateNickname(request.nickName());

		// 검증을 User객체 본인이 하게 만듬
		writer.validatePassword(request.password(), passwordEncoder);

		post.update(request.title(), request.text());

		return new PostResponse.Update(
			post.getId(),
			post.getUser().getNickname(),
			post.getTitle(),
			post.getText()
		);
	}

	public void migrate(Board fromBoard, Board toBoard) {
		postRepository.migrate(fromBoard, toBoard);
	}

	// 내부에서만 사용하는 메서드기에 private
	// 원래는 notice 엔티티에 update를 오버라이딩해서 검증하려했는데 삭제할 때도 검증해야하기때문에 검증을 service에 만들자
	private void validateUpdateDeletePermission(Post post, User currentUser) {
		if(post instanceof Notice) {
			if(currentUser.isAdmin()){
				throw new NoticeException(ExceptionCode.FORBIDDEN_ADMIN);
			}
		}
	}

	// Delete
	// 게시글 삭제 시 댓글들도 삭제되어야함
	public void delete(Long id, PostRequest.Delete request) {
		Post post = getEntityByPostId(id);

		// User를 만들어주면서 user 본인 객체에게 검증을 맡김
		User writer = post.getUser();

		writer.validateNickname(request.nickName());

		writer.validatePassword(request.password(), passwordEncoder);

		// 객체의 연관관계를 단방향으로 설정하면 됨
		// db관점에서는 이미 외래키로 연결되어 있어서 쿼리 날릴 때 연관해서 날아갈 수 있음
		postRepository.delete(post);
		// 게시판쪽 컬렉션에서도 삭제 List<Post> posts

		// jvm 메모리상 db와 불일치할 수 있음
		// jvm 컬렉션도 비워서 동기화 -> 리스트에 포함된 댓글들도 없어짐
		// 이렇게 해주면 Post의 removeComment 메서드 필요 없어짐
		// == 메모리 상태와 db의 상태를 맞춰주기 위해 List clear해줌
		post.getComments().clear();
		// 엔티티쪽에서 CascadeType.REMOVE로 둬서 댓글들도 자동 삭제됨
		// orphanRemoval=true로 둬서 부모와의 연관이 끊어진 댓글도 자동 삭제

		// post와 user, comment와 user는 양방향 매핑이라 컬렉션에서 지워줘야 함
		writer.getPosts().remove(post);
		post.getComments().forEach(comment -> comment.getUser().getComments().remove(comment));
	}
}
