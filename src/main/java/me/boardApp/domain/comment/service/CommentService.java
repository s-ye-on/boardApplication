package me.boardApp.domain.comment.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.boardApp.authorization.AuthorizationService;
import me.boardApp.domain.comment.Comment;
import me.boardApp.domain.comment.dto.CommentResponse;
import me.boardApp.global.CommonService;
import me.boardApp.global.exception.CommentException;
import me.boardApp.domain.comment.CommentRepository;
import me.boardApp.global.dto.request.CommentRequest;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
	private final CommentRepository commentRepository;
	private final PasswordEncoder passwordEncoder;
	private final CommonService commonService;
	private final AuthorizationService authorizationService;

	// Create

	public CommentResponse.Create create(Long postId, CommentRequest.Create request, Long currentUserId) {
		Post post = commonService.getPostById(postId);
		post.validateCanAddComment();

		User writer = commonService.getUserById(currentUserId);

		Comment comment = new Comment(
			post,
			writer,
			request.comment()
		);

		post.commented(comment);
		commentRepository.save(comment);

		return toCreateResponse(comment);
	}

	private CommentResponse.Create toCreateResponse(Comment comment) {
		return new CommentResponse.Create(
			comment.getId(),
			comment.getUser().getNickname(),
			comment.getComment(),
			comment.getCreatedDate()
		);
	}

	// Read
	// 한 게시글에 한 사람이 여러 댓글 남길수도 있으니 List 반환
	public Page<CommentResponse.Read> readAllByWriterAndPostId(String writerName, Long postId, Pageable pageable) {
//		return commentRepository.findAllByUserNicknameAndPostId(writerName, postId)
//			.stream()
//			.map(this::mapToCommentReadResponse)
//			.toList();
		/// todo : writerName 검증, postId 검증 로직
		// page로 전환
		// 사실 comment는 몇 개 없기에 List로 반환해도 됨
		return commentRepository.findAllByUserNicknameAndPostId(writerName, postId, pageable)
			.map(this::toReadResponse);
	}

	public CommentResponse.Read readByCommentId(Long commentId) {
		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new CommentException(ExceptionCode.NOT_FOUND_COMMENT));

		return toReadResponse(comment);
	}

	public List<CommentResponse.Read> readAllByPostId(Long postId) {
		// 슬라이스로 사용 시 이렇게 함 근데 이렇게 하면 댓글 더보기 이런 버튼을 눌러야하니 그냥 한눈에 보이게 List 반환하는걸로 만들겠음
//		Pageable pageable = PageRequest.of(0, size); // 첫 슬라이스, size만큼
//		Slice<Comment> slice = commentRepository.findByPostIdOrderByCreatedDateAsc(postId, pageable);
//		return slice.map(this::mapToCommentReadResponse);
		Post post = commonService.getPostById(postId);

		List<Comment> comments = commentRepository.findAllByPostIdOrderByCreatedDateAsc(post.getId());

		return comments.stream()
			.map(this::toReadResponse)
			.toList();
	}

	private CommentResponse.Read toReadResponse(Comment comment) {
		return new CommentResponse.Read(
			comment.getId(),
			comment.getUser().getNickname(),
			comment.getComment(),
			comment.getCreatedDate(),
			comment.getLastModifiedDate()
		);
	}

	// Update
	public CommentResponse.Update update(Long postId, Long commentId, CommentRequest.Update request, Long currentUserId) {
		Comment target = commentRepository.findById(commentId)
			.orElseThrow(() -> new CommentException(ExceptionCode.NOT_FOUND_COMMENT));

		if(!target.getPost().getId().equals(postId)) {
			throw new CommentException(ExceptionCode.NOT_MATCH_POST_COMMENT);
		}

		User writer = target.getUser();
		User currentUser = commonService.getUserById(currentUserId);

		// 1. 작성자 본인인지 확인 (관리자는 수정 불가 정책)
		authorizationService.checkOwner(writer, currentUser);

		// 2. 비밀번호 재확인 (현재 유저 기준으로)
		currentUser.validatePassword(request.password(), passwordEncoder);

		// 3. 실제 내용 수정
		target.update(request.comment());
		return toUpdateResponse(target);
	}

	private CommentResponse.Update toUpdateResponse(Comment comment) {
		return new CommentResponse.Update(
			comment.getId(),
			comment.getUser().getNickname(),
			comment.getComment(),
			comment.getCreatedDate(),
			comment.getLastModifiedDate()
		);
	}

	// Delete
	public void deleteByCommentId(Long id, CommentRequest.Delete request, Long currentUserId) {
		Comment target = commentRepository.findById(id)
			.orElseThrow(() -> new CommentException(ExceptionCode.NOT_FOUND_COMMENT));

		User writer = target.getUser();

		//1. 현재 로그인한 유저 조회
		User currentUser = commonService.getUserById(currentUserId);

		// 2. 도메인 규칙 : 작성자 본인 or 관리자 확인
		authorizationService.checkOwnerOrAdmin(writer, currentUser);

		// 3. 현재 로그인한 유저 기반 인증
		currentUser.validatePassword(request.password(), passwordEncoder);

		// 4. 삭제
		// Post쪽의 컬렉션에서만 삭제해줘도 orphanRemoval이 걸려있어서 commentRepository에 있는 comment도 자동으로 삭제 됨
//		commentRepository.delete(target);
		///  todo : 엔티티쪽에 헬퍼 메서드를 둬서 양방향 연관관계 정리하는거 만들어주자 (target.removeFromRelations())
		/// 이렇게 하면 서비스 코드에서 연관관계에 대해 세부 구현(user, post 컬렉션) 을 몰라도 됨
		// Post쪽 컬렉션에서 삭제
		target.getPost().getComments().remove(target);
		// User쪽 컬렉션에서도 삭제
		target.getUser().getComments().remove(target);

		///  flush도 지금 테스트때문에 존재 나중에 삭제
		commentRepository.flush();

	}

	// 수정하다 든 생각이 여긴 request에 어떤 유저에 대한 정보가 담겨야하는데
	// 이 경우 일반 유저가 타인의 글을 다 지우는 공격 루트가 될 것 같음
	// 특정 사용자의 댓글을 다 지운다? 이거는 위험해보임
	// 관리자만 접근할 수 있게 @PreAuthorization ADMIN만 주는게 맞을 것 같음
	// 여기서 또 고민이 생기는데 CommentService에 관리자 기능과 일반 유저 기능이 혼재하는게 맞나?
	//-> 일단 도메인 기준으로 나누는게 먼저임.
	// 누가 이 기능을 쓸 수 있냐는 서비스 레벨보다 컨트롤러 + 시큐리티(@PreAuthentication)에서 역할 분기로 처리하는게 맞을 것 같음
	// 관리자용 댓글 관리 기능이 커진다면 그땐 AdminCommentService로 나누자
	public void deleteAllByAdmin(CommentRequest.DeleteByAdmin request, Long currentUserId) {
		User writer = commonService.getUserByNickname(request.writer());

		User currentUser = commonService.getUserById(currentUserId);
		authorizationService.checkAdmin(currentUser);

		List<Comment> allComments = commentRepository.findALlByUserId(writer.getId());

		currentUser.validatePassword(request.password(), passwordEncoder);

		for (Comment comment : allComments) {
			// db에서 지워주는게 아니라 컬렉션에서 지워주면 부모(Post)에서 지워주면 orphanRemoval로 깔끔함
			comment.getPost().getComments().remove(comment);
			// user 컬렉션은 직접 지워주는게 필수는 아님
			// User에 orphanRemoval = true가 없음
			// db삭제와 무관하고 단순히 영속성 컨텍스트의 관계 정리용일뿐
			// db 삭제는 post쪽 orphanRemoval이 담당
			comment.getUser().getComments().remove(comment);
		}
		// 여기서 commentRepository.delete() 호출할 필요 없음
		// 이유 : orphanRemoval = true + 컬렉션에서 제거
		// jpa가 해당 comment 고아로 인식
		// 트랜잭션 커밋 시점에 자동으로 DELETE 쿼리 실행
	}
}
