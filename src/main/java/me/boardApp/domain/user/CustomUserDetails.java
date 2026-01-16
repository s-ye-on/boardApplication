package me.boardApp.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
	private final User user;

	public Long getId() {
		return user.getId();
	}

	public String getNickname() {
		return user.getNickname();
	}

	public String getEmail() {
		return user.getEmail();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// Role.USER , Role.ADMIN -> ROLE_USER, ROLE_ADMIN
		String roleName = "ROLE_" + user.getRole().name();
		return List.of(new SimpleGrantedAuthority(roleName));
	}

	@Override
	public String getPassword() {
		return user.getPassword(); // 이미 BCrypt로 인코딩된 값
	}

	@Override
	public String getUsername() {
		// 로그인 ID로 "이메일"을 쓰기로 하자 로그인할 떄 필요한 ID
		return user.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true; // 필요하면 Status 기반으로 제어 가능
	}

	@Override
	public boolean isAccountNonLocked() {
		return true; // 추후 잠금 기능 넣고 싶으면 여기서 제어
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true; // 비밀번호 만료 정책 사용 시 수정
	}

	@Override
	public boolean isEnabled() {
		// 탈퇴(비활성화) 된 유저는 로그인 못하게 하려면 여기서 체크
		return user.getStatus() == User.Status.ACTIVATION;
	}
}
