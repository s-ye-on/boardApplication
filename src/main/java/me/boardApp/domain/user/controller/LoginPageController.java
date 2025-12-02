package me.boardApp.domain.user.controller;

import lombok.RequiredArgsConstructor;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.domain.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class LoginPageController {

	private final UserService userService;

	// 로그인/회원가입 화면
	@GetMapping("/login")
	public String loginPage() {
		return "login"; // templates/login.html
	}

	// 폼 기반 회원가입 처리 (JSON 말고 form-data 기반)
	@PostMapping("/users/join-form")
	public String joinFromForm(UserRequest.Create request) {
		userService.join(request);
		// 회원가입 후 로그인 페이지로 다시 이동
		return "redirect:/login";
	}
}