package me.boardApp.domain.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.admin.service.AdminUserService;
import me.boardApp.log.ClientContextFilter;
import me.boardApp.log.dto.ClientContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

	private final AdminUserService adminUserService;

	@PostMapping("/{userId}/unlock")
	public ResponseEntity<Void> unlockUser(@PathVariable Long userId, HttpServletRequest request) {
		ClientContext clientContext = (ClientContext) request.getAttribute(ClientContextFilter.CLIENT_CONTEXT_KEY);
		// getAttribute로 값이 없을 때는 조용히 null을 반환한다

		adminUserService.unlockUser(userId, clientContext);

		return ResponseEntity.noContent().build();
	}
}
