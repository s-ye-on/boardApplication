package me.boardApp.domain.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//@Repository 동작하지 않음
// 왜?? proxy 패턴 때문에
public interface BoardRepository extends JpaRepository<Board, Long> {
	Optional<Board> findByName(String name);

	// 임시 게시판 존재 여부 확인
	// Spring Data JPA 제공하는 쿼리 메서드 네이밍 규칙덕에
	// 자동으로 SQL 생성
	boolean existsByType(Board.Type type);

	//임시 게시판 타입으로 찾기
	Optional<Board> findByType(Board.Type type);
}
