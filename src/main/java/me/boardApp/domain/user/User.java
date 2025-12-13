package me.boardApp.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.boardApp.BaseEntity;
import me.boardApp.domain.comment.Comment;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.domain.post.Post;
import me.boardApp.global.exception.UserException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Column(nullable = false, length = 50)
	private String realName;

	@Column(nullable = false, length = 50, unique = true)
	private String nickname;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(nullable = false, length = 50, unique = true)
	private String email;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Status status;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Role role;

	/// todo
	/// 댓글은 회원 탈퇴되더라도 "탈퇴한 회원"으로 댓글 보이게 하고 싶음
	/// 하지만 게시글은 회원 탈퇴 시 게시글이 안보이게 하고 싶음
	/// 여기서 방법이 두가지 있는데 1. DB에서 실제로 삭제(hard delete) 2. DB에는 남아 있지만 사용자에게만 보이게 하기(soft delete)
	/// 2번으로 할 경우 user처럼 status를 두면 됨
	///  이거는 나중에 고민해보고 결정

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Post> posts = new ArrayList<>();

	// 나는 일단 user가 삭제되더라도 댓글과 글이 그대로 있게 하고 싶음 (에타처럼) 그래서 CascadeType과 orphanRemoval을 빼주는게 맞음
	// 하지만 게시글은 회원이 탈퇴하면 저절로 삭제되게 하고 싶음
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private List<Comment> comments = new ArrayList<>();

	public User(String realName, String nickname, String password, String email) {
		this.realName = realName;
		this.nickname = nickname;
		this.password = password;
		this.email = email;
		this.status = Status.ACTIVATION;
		this.role = Role.USER;
	}

	public static User createAdmin(String realName, String nickname, String password, String email) {
		User admin = new User(realName, nickname, password, email);
		admin.role = Role.ADMIN;
		return admin;
	}

	// 유저 객체 간 비교를 하는 케이스는 실제로 잘 안씀
	// 보통은 password, email 처럼 각각 독립적으로 검증하는게 명확
	public void validateCredentials(User user, PasswordEncoder passwordEncoder) {
		if (!this.nickname.equals(user.nickname) || !passwordEncoder.matches(user.password, this.password)) {
			throw new UserException(ExceptionCode.USER_VALIDATION_FAILED);
		}
	}

	public void validateNickname(String nickname) {
		if (!nickname.equals(this.nickname)) {
			throw new UserException(ExceptionCode.INVALID_NICKNAME);
		}
	}

	public void validateRealName(String realName) {
		if (!Objects.equals(this.realName, realName)) {
			throw new UserException(ExceptionCode.INVALID_REAL_NAME);
		}
	}
	// NPE 방지를 위해 위에걸로 바꿈 -> 내부적으로 null-safe 비교해줌
	//	public void validateRealName(String realName){
//		if(!this.getRealName().equals(realName)){
//			throw new IllegalArgumentException("이름이 일치하지 않습니다");
//		}
//	}

	public void validatePassword(String password, PasswordEncoder passwordEncoder) {
		if (!passwordEncoder.matches(password, this.password)) {
			throw new UserException(ExceptionCode.INVALID_PASSWORD);
		}
	}

	public void validateEmail(String email) {
		if (!this.getEmail().equals(email)) {
			throw new UserException(ExceptionCode.INVALID_EMAIL);
		}
	}

	public boolean isAdmin() {
		return this.role == Role.ADMIN;
	}

	public void updateNickname(String newNickname) {
		this.nickname = newNickname;
	}

	public void updatePassword(String password) {
		this.password = password;
	}

	public void updateEmail(String email) {
		this.email = email;
	}

	public void validateDelete(UserRequest.Delete request, PasswordEncoder passwordEncoder) {
		this.validatePassword(request.password(), passwordEncoder);
		this.validateEmail(request.email());
		this.validateRealName(request.realName());
	}

	public void inactivate() {
		this.status = Status.INACTIVATION;
		this.nickname = "탈퇴한 회원" + this.getId();
	}

	public void activate(String nickname) {
		this.status = Status.ACTIVATION;
		this.nickname = nickname;
	}

	// soft delete에서 사용
	public enum Status {
		ACTIVATION,
		INACTIVATION
	}

	// enum 정해진 개수의 상태값 중 하나
	// record 여러 값을 묶은 불변 데이터 구조(DTO,응답 객체, 복합 값 표현)
	public enum Role {
		USER,
		ADMIN
	}
}
