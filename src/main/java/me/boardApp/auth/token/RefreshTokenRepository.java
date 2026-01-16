package me.boardApp.auth.token;

import me.boardApp.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByToken(String token);

	Optional<RefreshToken> findByUser(User user);

	Optional<RefreshToken> findTopByUserOrderByIdDesc(User user);

	List<RefreshToken> findAllByUserAndStatus(User user, RefreshToken.Status status);

	@Modifying
	@Query("""
		update RefreshToken rt
		set rt.status = 'REVOKED'
		where rt.user = :user
		""")
	void revokeAllByUser(@Param("user") User user);

	void deleteByUser(User user);
}
