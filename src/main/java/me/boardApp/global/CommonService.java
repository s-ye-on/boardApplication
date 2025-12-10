package me.boardApp.global;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.board.Board;
import me.boardApp.domain.board.BoardRepository;
import me.boardApp.domain.comment.CommentRepository;
import me.boardApp.domain.post.Post;
import me.boardApp.domain.post.PostRepository;
import me.boardApp.domain.user.User;
import me.boardApp.domain.user.UserRepository;
import me.boardApp.global.exception.BoardException;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.global.exception.PostException;
import me.boardApp.global.exception.UserException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class CommonService {
	// CommonService를 인터페이스로 두고 각 service가 상속받는 구조로 만들면 안될까?
	// x 문제점 : 1. 단일책임 위반 2. 순환참조 해결 목적과 어긋남 3. 서비스 인터페이스 의미 약화
	// commonService는 순환참조를 해결하기 위해 존재하기 때문에 조회를 모조리 다 넣을 필요는 없음
	// 다른 서비스에서 참조해야하는 최소한의 기능만 넣으면 됨

	// 단일 책임에 충실하게 "조회 + 예외 처리"만 담당하도록 만들어봄
	// 이 조건이 안지켜진다면 쓰레기통 service가 될 것임
	private final BoardRepository boardRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;

	// board 관련
	public Board getBoardById(Long boardId) {
		return boardRepository.findById(boardId)
			.orElseThrow(()-> new BoardException(ExceptionCode.NOT_FOUND_BOARD));
	}

	// post 관련
	public Post  getPostById(Long postId) {
		return postRepository.findById(postId)
			.orElseThrow(()-> new PostException(ExceptionCode.NOT_FOUND_POST));
	}

	// comment 관련

	// user 관련
	public User getUserByNickname(String nickname) {
		return userRepository.findByNickname(nickname)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_NICKNAME));
	}

	public User getUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));
	}

	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));
	}

	public void validateAdmin(User user) {
		if(!user.isAdmin()) {
			throw new UserException(ExceptionCode.FORBIDDEN_ADMIN);
		}
	}
}
