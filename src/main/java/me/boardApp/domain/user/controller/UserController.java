package me.boardApp.domain.user.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.post.dto.PostResponse;
import me.boardApp.domain.post.service.PostService;
import me.boardApp.domain.user.CustomUserDetails;
import me.boardApp.domain.user.dto.UserResponse;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.domain.user.service.UserService;
import me.boardApp.global.response.SuccessMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
	private final UserService userService;
	private final PostService postService;

	@PostMapping("/join")
	@ResponseStatus(HttpStatus.CREATED)
	public void joinUser(@RequestBody @Valid UserRequest.Create request) {
		userService.join(request);
	}

	@PostMapping("/rejoin")
	public void reJoin(@RequestBody @Valid UserRequest.ReJoin request) {
		userService.reJoin(request);
	}

	// 다 좋은데 API응답으로 엔티티 자체(User)를 직접 반환하는건 피하는게 좋음
	// 이유 : 1. 보안 문제 - 비밀번호, 내부 ID, 권한 같은 민감 정보가 노출될 수 있음
	// 2. 유연성 저하 - 나중에 User 엔티티 구조가 바뀌면 API 스펙까지 깨짐
	// 3. 계층 분리 위반 - 엔티티는 Db 계층의 모델이지, API 응답 모델이 아님
	@PostMapping("/login")
	public ResponseEntity<UserResponse.Login> login(@RequestBody @Valid UserRequest.Login request) {
		var response = userService.login(request);

		return ResponseEntity
			.status(SuccessMessage.LOGIN_SUCCESS.getStatus())
			.body(response.withMessage(SuccessMessage.LOGIN_SUCCESS.getMessage()));
	}

	// 로그아웃은 세션을 끊는 역할밖에 하지 않음
	// 비즈니스 로직이 없는 단순한 기술 처리
	// -> Service 계층을 거칠 필요가 없음
	// 왜 서비스 계층 거치지 않나?
	// 서비스는 일반적으로 도메인로직(회원가입, 게시글 등록, 댓글 검증 등), 비즈니스 규칙, 트랜잭션 관리 를 담당함
	// 근데 로그아웃은 그런게 아니라 세션을 무효화하는 단순한 기술적 동작
	// 그래서 Controller에서 바로 처리하는게 깔끔 -> 단순 컨트롤러 책임의 예외 케이스
	// 만약 로그아웃 하면서 다음 같은 로직이 필요하다면 Service로 옮기는게 맞음
	// 1. 로그아웃 기록을 db에 저장, 토큰 블랙리스트 등록(jwt 기반), Redis 세션 제거, 마지막 접속 시간 업데이트 등
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session) {
		session.invalidate();

		return ResponseEntity
			.status(SuccessMessage.LOGOUT_SUCCESS.getStatus())
			.body(SuccessMessage.LOGOUT_SUCCESS.getMessage());
	}

	@GetMapping("/{userNickname}/posts")
	public Page<PostResponse.Read> getUserPosts(@PathVariable String userNickname,
																							@PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC)
																							Pageable pageable) {
		return postService.readByWriter(userNickname, pageable);
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping("/nickname")
	public void updateNickname(@RequestBody @Valid UserRequest.UpdateNickname request,
														 @AuthenticationPrincipal CustomUserDetails userDetails) {
		userService.updateNickname(request, userDetails.getId());
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping("/password")
	public void updatePassword(@RequestBody @Valid UserRequest.UpdatePassword request,
														 @AuthenticationPrincipal CustomUserDetails userDetails) {
		userService.updatePassword(request, userDetails.getId());
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping("/email")
	public void updateEmail(@RequestBody @Valid UserRequest.UpdateEmail request,
													@AuthenticationPrincipal CustomUserDetails userDetails) {
		userService.updateEmail(request, userDetails.getId());
	}

	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/delete")
	public void deleteMe(@RequestBody @Valid UserRequest.Delete request,
											 @AuthenticationPrincipal CustomUserDetails userDetails) {
		userService.delete(request, userDetails.getId());
	}
}
