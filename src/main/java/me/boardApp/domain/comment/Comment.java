package me.boardApp.domain.comment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.boardApp.BaseEntity;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.user.User;

// 2가지 검증이 존재함.
// 1. 값 검증 : 이 값 자체가 정상적인가? (ex. 주문 금액 -> 0원, -1000원 ❌)
// 2. 비지니스 검증 : 이 값이 사용될 수 있는가? (ex. 최소 주문 금액을 맞춰야함 24,000원 이상)

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {
	@Column(nullable = false, length = 200)
	private String comment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public Comment(Post post, User user, String comment) {
		this.post = post;
		this.user = user;
		this.comment = comment;
	}

	public void update(String comment) {
		this.comment = comment;
	}
}
