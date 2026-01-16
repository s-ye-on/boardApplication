package me.boardApp.domain.board;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.boardApp.BaseEntity;
import me.boardApp.global.exception.BoardException;
import me.boardApp.global.exception.ExceptionCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@EntityListeners(AuditingEntityListener.class) 이미 base 엔티티에 있으니 상속 받은 엔티티에서까지 있을 필요 없음
public class Board extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Type type; // 게시판 종류

	// 다양한 기능을 가진 게시판을 만드려면 (공지사항, 자유게시판 등등)
	// 단일 Board 엔티티 + inner class (enum class)로 type 넣기

	// orphanRemoval : 컬렉션에서도 제거가 되어야 DB에서도 삭제 반영
	// 연관관계 단방향 전환
//	@OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval =true)
//	List<Post> posts = new ArrayList<>();

	public Board(String name, String description, Type type) {
		this.name = name;
		this.description = description;
		this.type = type;
	}

	// 엔티티 내부에서만 상태를 변경할 수 있게해서 캡슐화 지키려함
	// BoardService에서 Setter 안쓸수 있어서 캡슐화 유지
	// optional 사용하던 것 수정 -> optional은 생성된 곳 혹은 외부에서 받아온 곳에서 바로 소비해야함
	public void update(String name, String description) {
		if (this.type == Type.TEMPORARY) {
			throw new BoardException(ExceptionCode.CANNOT_UPDATE_TEMP_BOARD);
		}
		this.name = name;
		this.description = description;
	}

	//	public void posted(Post post){
//		posts.add(post);
//	}
	public enum Type {
		TEMPORARY,
		FREE,
		TEST;

		public boolean isTemporary() {
			return this == Type.TEMPORARY;
		}
	}
}
