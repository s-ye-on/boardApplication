package me.boardApp.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	// repository와 엔티티는 같은 패키지에 있어야함
	// repository는 컬렉션
	// 그 대상을 관리하는 친구, User를 관리하는 컬렉션에서 user가 나와야지 게시글이 나올 순 없음
	// N+1 문제 때문에 JPQL 사용
	// @OneToMany 사용 시 N+1 문제 발생 확률 증가
	@Query("SELECT u FROM User u JOIN FETCH u.posts")
	List<User> findAllWithPosts();

	@Query("SELECT u FROM User u JOIN FETCH u.comments")
	List<User> findAllWithComments();

	Optional<User> findByEmail(String email);

	Optional<User> findByNickname(String nickname);

	boolean existsByNickname(String nickname);

	// 특정 조건에 따라 페이징 적용
	// nickname에 따라
	Page<User> findByNicknameContaining(String nickname, Pageable pageable);
}
