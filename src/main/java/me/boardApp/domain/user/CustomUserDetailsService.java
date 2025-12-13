package me.boardApp.domain.user;

import lombok.RequiredArgsConstructor;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.UserException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	// username email 사용(로그인 ID를 말하는 것임)
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// username에는 /login 폼에서 입력한 값이 들어옴
		User user = userRepository.findByEmail(username)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		return new CustomUserDetails(user);
	}
}
