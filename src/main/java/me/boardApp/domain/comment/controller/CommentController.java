package me.boardApp.domain.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.comment.dto.CommentResponse;
import me.boardApp.domain.comment.service.CommentService;
import me.boardApp.global.dto.request.CommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {
	private final CommentService commentService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse.Create create(
		@RequestParam Long postId, // ?key=value 형식으로 전달
		@RequestBody @Valid CommentRequest.Create request
	) {
		return commentService.create(postId, request);
	}

	@GetMapping("/{id}")
	public CommentResponse.Read findById(@PathVariable Long id) {
		return commentService.readByCommentId(id);
	}

	// /posts/1/comments?writerName=승연 처럼 호출 가능
	@GetMapping("/search")
	public Page<CommentResponse.Read> findAllByWriterNameAndPostId(
		// 검색 조건이러 @RequestParam 사용
		// 하지만 findAllByPostId와 url 충돌 나서 바꿔줌
		@RequestParam Long postId,
		// 검색 조건이라 @RequestParam으로 수정
		@RequestParam String writerName,
		// 어떤 글의 댓글인지 식별 -> @PathVariable 사용
		@PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
		return commentService.readByWriterAndPostId(writerName, postId, pageable);
	}

	@GetMapping
	// comment 같은 경우는 post처럼 엄청 많지도 않고 페이지 번호로 이동할 필요가 없으니
	// page 대신 slice로 만들어주는게 좋음
	public List<CommentResponse.Read> findAllByPostId(
		@RequestParam Long postId
		// @RequestParam(defaultValue = "10") int size
	) {
		return commentService.readAllByPostId(postId);
	}

	@PatchMapping("/{id}")
	public CommentResponse.Update updateComment(
		@PathVariable Long id,
		@RequestBody @Valid CommentRequest.Update commentUpdateRequest
	) {
		return commentService.updateComment(id, commentUpdateRequest);
	}

	// URL의 {id}로 삭제할 리소스(댓글)를 명확히 식별
	// Body(commentDeleteRequestById)에는 삭제 검증용 데이터(작성자, 비밀번호 등)을 담을 수 있음
	// -> 리소스 식별은 PathVariable, 추가적인 검증 데이터는 Body -> 깔끔!
	@DeleteMapping("/{id}")
	public void deleteById(
		@PathVariable Long id,
		@RequestBody @Valid CommentRequest.Delete commentDeleteRequest
	) {
		commentService.deleteByCommentId(id, commentDeleteRequest);
	}

	@DeleteMapping
	public void deleteAllByWriter(@RequestBody @Valid CommentRequest.Delete commentDeleteRequest) {
		commentService.deleteAllByWriter(commentDeleteRequest);
	}
}
