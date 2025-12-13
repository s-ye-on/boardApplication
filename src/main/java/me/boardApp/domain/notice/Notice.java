package me.boardApp.domain.notice;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.user.User;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.NoticeException;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("NOTICE") // 각 하위 클래스의 구분 값 지정
public class Notice extends Post {

	public Notice(Board board, User user, String title, String text) {
		super(board, user, title, text); // 부모 생성자 호출
	}

	// 부모의 update를 그대로 호출한다면 오버라이딩을 할 필요가 없음
	@Override
	public void update(String title, String text) {
		if (getUser().isAdmin()) {
			throw new NoticeException(ExceptionCode.FORBIDDEN_ADMIN);
		}
		super.update(title, text);
	}

	@Override
	public void validateCanAddComment() {
		throw new NoticeException(ExceptionCode.CANNOT_COMMENT_NOTICE_POST);
	}
}
