package me.boardApp.domain.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	// comment는 자식과 부모가 모두 존재해야 조회할 수 있는 fetch join(inner join)이 맞음
	// 게시글이 없는 상태에서 댓글이 조회되면 안됨
	Page<Comment> findAllByUserNicknameAndPostId(String writer, Long postId, Pageable pageable);

	List<Comment> findAllByPostId(Long postId);

	List<Comment> findAllByPostIdOrderByCreatedDateAsc(Long postId);

	List<Comment> findAllByUserNickname(String writer);

	List<Comment> findALlByUserId(Long userId);

	// slice로 조회
	Slice<Comment> findByPostIdOrderByCreatedDateAsc(Long postId, Pageable pageable);
}
