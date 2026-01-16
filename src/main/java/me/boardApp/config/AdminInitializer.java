package me.boardApp.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@PostConstruct
	public void initAdmin() {
		boolean existsAdmin = userRepository.existsByRole(User.Role.ADMIN);
		if (existsAdmin) {
			return;
		}

		User admin = User.createAdmin(
			"관리자",
			"admin",
			// 테스트에선 하드 코딩 괜찮지만
			// 실서비스에선 환경변수로 받아야함
			// 운영 시작 후 즉시 비밀번호 변경 요망
			passwordEncoder.encode("admin1234"),
			"csy0318@naver.com"
		);
		userRepository.save(admin);
		log.info("[BOOTSTRAP] Initial admin account created.");
	}
}
