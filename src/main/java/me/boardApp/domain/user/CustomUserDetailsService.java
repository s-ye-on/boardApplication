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

	// username nickname으로 사용
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// username에는 /login 폼에서 입력한 값이 들어옴
		// warp는 이메일을 아이디로 쓰자 했지만 나는 nickname을 아이디로 사용하고 싶음
		User user = userRepository.findByNickname(username)
			.orElseThrow(()-> new UserException(ExceptionCode.NOT_FOUND_USER));

		return new CustomUserDetails(user);
	}
}
