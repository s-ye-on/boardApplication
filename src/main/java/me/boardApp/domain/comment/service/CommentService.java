package me.boardApp.domain.comment.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

	// Create
	public CommentResponse.Create create(Long postId, CommentRequest.Create request) {
		Post post = commonService.getPostById(postId);
		post.validateCanAddComment();

		User writer = commonService.getUserByNickname(request.writer());

		Comment comment = new Comment(
			post,
			writer,
			request.comment()
		);

		post.commented(comment);
		commentRepository.save(comment);

		return mapToCommentCreateResponse(comment);
	}

	public CommentResponse.Create mapToCommentCreateResponse(Comment comment) {
		return new CommentResponse.Create(
			comment.getId(),
			comment.getUser().getNickname(),
			comment.getComment(),
			comment.getCreatedDate()
		);
	}

	// Read
	// 한 게시글에 한 사람이 여러 댓글 남길수도 있으니 List 반환
	public Page<CommentResponse.Read> readByWriterAndPostId(String writerName, Long postId, Pageable pageable) {
//		return commentRepository.findAllByUserNicknameAndPostId(writerName, postId)
//			.stream()
//			.map(this::mapToCommentReadResponse)
//			.toList();
		/// todo : writerName 검증, postId 검증 로직
		// page로 전환
		// 한 유저가 어떤 한 게시글에 단 댓글을 보여주는거니까 List로 받아주는게 더 가시성이 있어보이기도 하고.. 고민
		return commentRepository.findAllByUserNicknameAndPostId(writerName, postId, pageable)
			.map(this::mapToCommentReadResponse);
	}

	public CommentResponse.Read readByCommentId(Long commentId) {
		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new CommentException(ExceptionCode.NOT_FOUND_COMMENT));

		return mapToCommentReadResponse(comment);
	}

	//Slice로 조회
	public List<CommentResponse.Read> readAllByPostId(Long postId) {
		// 슬라이스로 사용 시 이렇게 함 근데 이렇게 하면 댓글 더보기 이런 버튼을 눌러야하니 그냥 한눈에 보이게 List 반환하는걸로 만들겠음
//		Pageable pageable = PageRequest.of(0, size); // 첫 슬라이스, size만큼
//		Slice<Comment> slice = commentRepository.findByPostIdOrderByCreatedDateAsc(postId, pageable);
//		return slice.map(this::mapToCommentReadResponse);
		Post post = commonService.getPostById(postId);

		List<Comment> comments = commentRepository.findAllByPostIdOrderByCreatedDateAsc(post.getId());

		return comments.stream()
			.map(this::mapToCommentReadResponse)
			.toList();
	}

	public CommentResponse.Read mapToCommentReadResponse(Comment comment) {
		return new CommentResponse.Read(
			comment.getId(),
			comment.getUser().getNickname(),
			comment.getComment(),
			comment.getCreatedDate(),
			comment.getLastModifiedDate()
		);
	}

	// Update
	public CommentResponse.Update updateComment(Long commentId, CommentRequest.Update request) {
		Comment target = commentRepository.findById(commentId)
			.orElseThrow(()-> new CommentException(ExceptionCode.NOT_FOUND_COMMENT));

		User writer = commonService.getUserByNickname(request.writer());

		writer.validatePassword(request.password(), passwordEncoder);

		target.update(request.comment());
		return mapToCommentUpdateResponse(target);
	}

	public CommentResponse.Update mapToCommentUpdateResponse(Comment comment) {
		return new CommentResponse.Update(
			comment.getId(),
			comment.getUser().getNickname(),
			comment.getComment(),
			comment.getCreatedDate(),
			comment.getLastModifiedDate()
		);
	}

	// Delete
	public void deleteByCommentId(Long id, CommentRequest.Delete request) {
		Comment target = commentRepository.findById(id)
			.orElseThrow(()-> new CommentException(ExceptionCode.NOT_FOUND_COMMENT));

		User writer = commonService.getUserByNickname(request.writer());

		writer.validatePassword(request.password(), passwordEncoder);

			// Post쪽의 컬렉션에서만 삭제해줘도 orphanRemoval이 걸려있어서 commentRepository에 있는 comment도 자동으로 삭제 됨
//		commentRepository.delete(target);
		// Post쪽 컬렉션에서 삭제
		target.getPost().getComments().remove(target);
		commentRepository.flush();

		// User쪽 컬렉션에서도 삭제
		target.getUser().getComments().remove(target);
	}

	public void deleteAllByWriter(CommentRequest.Delete request) {
		User writer = commonService.getUserByNickname(request.writer());

		List<Comment> allComments = commentRepository.findAllByUserNickname(request.writer());

		writer.validatePassword(request.password(), passwordEncoder);

		for(Comment comment : allComments) {
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
