package me.boardApp.auth.token;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.boardApp.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// User 와의 관계 : 단방향 ManyToOne (또는 userId Long 으로만 저장해도 됨)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, unique = true, length = 255)
	private String token;

	@Column
	private LocalDateTime expiryDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Status status;

	protected RefreshToken(User user, String token, LocalDateTime expiryDate) {
		this.user = user;
		this.token = token;
		this.expiryDate = expiryDate;
		this.status = Status.ACTIVE;
	}

	public static RefreshToken create(User user, String token, LocalDateTime expiryDate) {
		return new RefreshToken(user, token, expiryDate);
	}

	public boolean isExpired() {
		return expiryDate.isBefore(LocalDateTime.now());
	}

	public boolean isActive() {
		return this.status == Status.ACTIVE && !isExpired();
	}

	public void revoke() {
		this.status = Status.REVOKED;
	}

	public void rotate(String newToken, LocalDateTime newExpiryDate) {
		this.token = newToken;
		this.expiryDate = newExpiryDate;
		this.status = Status.ACTIVE;
	}

	// rotate 사용으로 이제 안씀
	public void updateToken(String newToken, LocalDateTime newExpiryDate) {
		this.token = newToken;
		this.expiryDate = newExpiryDate;
	}

	// enum Status를 재발급하면 새걸로 덮으니 없어도 될거라 생각했는데,
	// 재사용 감지라는 보안적 측면때문에 사용하기로 함
	public enum Status {
		ACTIVE,
		REVOKED
	}
}
