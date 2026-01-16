package me.boardApp.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueNicknameValidator implements ConstraintValidator<UniqueNickname, String> {
	private final UserRepository userRepository;

	@Override
	public boolean isValid(String nickname, ConstraintValidatorContext context) {
		if (nickname == null || nickname.isBlank()) {
			return true; // 다른 @NotBlank 등이 처리하니까 여기선 통과시킴
		}
		return !userRepository.existsByNickname(nickname);
	}
}
