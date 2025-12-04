package me.boardApp.domain.post.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.post.dto.PostResponse;
import me.boardApp.domain.post.service.PostService;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.global.dto.request.PostRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards/{boardId}/posts")
public class PostController {
	private final PostService postService;

	@PreAuthorize("hasRole('USER')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse.Create createPost(@RequestBody @Valid PostRequest.Create request,
																				@AuthenticationPrincipal CustomUserDetails userDetails) {
		return postService.create(request, userDetails.getId());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/notices")
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse.Create createNotice(@RequestBody @Valid PostRequest.Create request,
																					@AuthenticationPrincipal CustomUserDetails userDetails) {
		return postService.createNotice(request, userDetails.getId());
	}

	// 오프셋 기반 페이징 : GET /boards/{boardId}/posts
	@GetMapping
	public Page<PostResponse.Read> getAllPosts(
		@PathVariable Long boardId,
		// pageable에 조건들이 자동주입되긴 하겠지만 조건 없이 요청 올 경우를 대비해 디폴트를 최신순으로 지정
		@PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC)
		Pageable pageable) {
		return postService.readAllByBoardId(boardId, pageable);
	}
	// pageable 자동 주입 때문에 몇페이지 몇개 글을 가져올 지 알아서 됨
	// GET /posts?page=0&size=10 이 요청이 들어오면 Spring이 자동으로 Pageable pageable = PageRequest.of(0, 10);로 만들어줌

	// 커서 기반 페이징 : Get /boards/{boardId}/posts/cursor
	// 장점 : 데이터가 많아도 안정적, 무한 스크롤에 적합
	@GetMapping("/cursor")
	public List<PostResponse.Read> getPosts(
		@RequestParam(required = false) Long lastPostId,
		@PageableDefault(size = 10, sort = "id",  direction = Sort.Direction.DESC) Pageable pageable) {

		return postService.readAllPostsCursor(lastPostId, pageable);
	}

	@GetMapping("/{id}")
	public PostResponse.Read getPost(@PathVariable Long id) {
		return postService.readByPostId(id);
	}

	// 기능이 겹침 getAllPosts와 겹침
//	@GetMapping
//	public Page<PostResponse.Read> getPostsByBoardId(@PathVariable Long boardId,
//																									@PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC)
//																										// @PageableDefault는 "기본값을 지정하기 위해 사용
//																										// 프론트에서 아무 파라미터를 보내지 않는다면 이렇게 기본값으로 처리할게 라는 뜻
//																										// 프론트에서 보낸 값이 있다면 그걸 우선으로 사용
//																										// md에서 더 자세히..
//																									Pageable pageable) {
//		return postService.readAllByBoardId(boardId, pageable);
//	}

	@GetMapping("/notices")
	public List<PostResponse.Read> getAllNoticeByBoardId(@PathVariable Long boardId) {
		return postService.readAllNoticeByBoardId(boardId);
	}

	// ("/posts?title={title}") x -> /posts 로만 둬도 쿼리 스트링은 자동 매핑 됨
	// String이 title 쿼리 파라미터를  @RequestParam으로 받아줌
	// 여기서는 @PageableDefault를 붙일까 말까?
	// 프론트에 달렸음 1. 프론트가 직접 page, size, sort를 넘긴다면 안붙임
	// 2. GET /posts/search?title=스프링 그냥 이렇게 검색어만 준다면 붙여야함
	// getPostsByTitle() 엔드포인트는 검색 API니까
	// 대부분 프론트에서 검색창 단어 입력 -> 결과 가져오기 식으로 동작할 가능성 높음
	// 그렇다면 @PageableDefault 붙여주는게 안전하고 자연스러움
	@GetMapping("/search")
	public Page<PostResponse.Read> getPostsByTitle(@RequestParam String title,
																								@PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC)
																								Pageable pageable) {
		return postService.readByTitle(title, pageable);
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public PostResponse.Update update(
		@PathVariable Long id,
		@RequestBody @Valid PostRequest.Update request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return postService.update(id, request, userDetails.getId());
	}

	@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT) // 삭제 성공, body 없음
	public void delete(
		@PathVariable Long id,
		@RequestBody @Valid PostRequest.Delete postDeleteRequest,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		postService.delete(id, postDeleteRequest, userDetails.getId());
	}
}
