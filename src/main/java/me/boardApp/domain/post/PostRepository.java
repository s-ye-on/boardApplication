package me.boardApp.domain.post;

import me.boardApp.domain.board.Board;
import me.boardApp.domain.notice.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
	// post는 자식과 부모가 모두 존재해야 조회할 수 있는 fetch join(inner join)이 맞음
	// 게시판이 없는 상태에서 댓글이 조회되면 안됨
	// 게시판 삭제해도 임시 게시판으로 게시글이 옮겨가기 때문에 상관 x
	// Modifying은 db에서 바로 값들을 변경해주니, 영속성 컨텍스트와 불일치하게 됨
	// 이럴 떄 영속성 컨텍스트를 한 번 비워주고 다시 불러오면 값들이 일치하게 됨
	@Modifying(clearAutomatically = true)
	// post 엔티티가 갖고 있는 board 참조(연관관계)를 다른 board 엔티티로 변경하는 쿼리
	@Query("update Post p set p.board =:newBoard where p.board =:oldBoard")
	void migrate(@Param("oldBoard") Board oldBoard, @Param("newBoard") Board newBoardId);

	// 엔티티 연관관계만 연결 되어 있다면 이렇게 해놔도
	// JPA가 자동으로 board.id=? 조건으로 쿼리 만들어줌
	Page<Post> findAllByBoardId(Long boardId, Pageable pageable);

	Page<Post> findAllByTitle(String title, Pageable pageable);

	Page<Post> findAllByUserNickname(String nickname, Pageable pageable);

	@Query("SELECT n FROM Notice n WHERE n.board.id = :boardId ORDER BY n.createdDate DESC")
	List<Notice> findALlNoticeByBoardId(@Param("boardId") Long boardId);

	// 커서 기반 페이징에서 아래 두개 사용
	// 처음 조회 : 최신 글부터 page size 만큼
	Page<Post> findTop10ByOrderByIdDesc(Pageable pageable);

	// 다음 페이지 : lastPostId 보다 작은 것들 중에서 page size 만큼
	Page<Post> findByIdLessThanOrderByIdDesc(Long lastPostId, Pageable pageable);
}
