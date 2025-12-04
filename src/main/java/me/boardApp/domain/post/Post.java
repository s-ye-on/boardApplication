package me.boardApp.domain.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.boardApp.BaseEntity;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.comment.Comment;
import me.boardApp.domain.user.User;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity // 엔티티의 정의? 테이블과 매핑된 Class (객체 != class)
@Table(name = "posts")
//@AllArgsConstructor(access = AccessLevel.PRIVATE) -> 모든 필드를 다 받는 것 필요하지 않음
// views 같은 건 기본 값 0L을 강제해야함 @AllArgsConstructor 사용 시 외부에서 views를 임의로 줄 수 있어서 설계 의도와 어긋 남
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 구분 컬럼 이름 지정
@DiscriminatorColumn(name = "post_type")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // 싱글 테이블 전략
public class Post extends BaseEntity {
	// 프로퍼티 순서 : public -> package-private (Default) -> protected -> private
	// 상수 -> 프로퍼티 -> 생성자 -> 메서드

	// 양방향 매핑의 주인은 외래키를 갖고 있는쪽이 주인(외래키인 user_id를 갖고 있는 Post가 주인)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// @NotBlank -> dto에 붙어 있고 엔티티가 검증할 일은 아님
	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private String text;

	@Column(nullable = false)
	private Long views;

	// @ManyToOne은 기본적으로 EAGER지만 실무에서는 LAZY로 바꿔서 씀
	// 불필요한 쿼리 실행, N+1문제, JPQL fetch join 제어 불가
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;

	// cascade는 부모쪽에 두는게 맞음 부모 -> 자식 방향으로 설정하는게 원칙
	// mappedBy 매핑은 부모, 즉 post와 매핑해야 함
	// 양방향 에서는 fk 를 가진쪽이 연관관계의 주인임
	// 현재 여기에서는 Comment가 post_id로 외래키를 갖고 있음
	// 1:N 관계의 N측이 FK를 갖음
	// 주인만이 @JoinColumn가질 수 있음
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comment> comments;

	//명시적 생성자
	// JPA 엔티티를 DB에서 불러올 때 1. 리플렉션으로 기본 생성자를 통해 객체를 만듬
	// 2. DB 컬럼 값을 필드에 세팅
	// -> JPA 엔티티는 "반드시 매개변수 없는 생성자(기본 생성자)가 있어야 함
	// 그걸 이제 @NoArgsConstructor(access = AccessLevel.PROTECTED) 이게 해줌
	public Post(Board board, User user , String title, String text) {
		this.board = board;
		this.user = user;
		this.title = title;
		this.text = text;
		this.views = 0L;
		this.comments = new ArrayList<>();
	}

	// 작성자 검증 로직은 writer쪽에서 자기 자신을 검증하는게 좋음
//	public void validateWriterAndPassword(String writer,  String inputPassword, PasswordEncoder passwordEncoder) {
//		if (!this.writer.equals(writer) || !passwordEncoder.matches(inputPassword, this.password)) {
//			throw new IllegalArgumentException("작성자 이름이 다르거나, 비밀번호가 틀렸습니다");
//		}
//	}

	// 엔티티 : 연관관계 처리 + 자기 자신이 책임져야할 로직
	// 서비스 : 트랜잭션 단위 묶기 + 여러 엔티티 협력 조율
	public void commented(Comment comment) {
		this.comments.add(comment);
	}

	// 캡슐화란? 내부의 요소를 외부로 드러내지 않음 -> 외부로 내부의 `디자인적 요소`를 노출시키지 않음
	// incrementViews -> viewed
	public void viewed() {
		this.views += 1;
	}

	// optional은 생성된 곳 혹은 외부에서 받아온 곳에서 바로 소비해야함.
	// 파라미터로 넘기지 말 것.
	public void update(String title, String text) {
		this.title = title;
		this.text = text;
	}

	public boolean isWriter(User user) {
		return this.user.equals(user);
	}

	public void validateCanAddComment() {
	}
}
