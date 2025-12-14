package me.boardApp.auth.token;

import me.boardApp.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByToken(String token);

	Optional<RefreshToken> findByUser(User user);

	Optional<RefreshToken> findTopByUserOrderByIdDesc(User user);

	List<RefreshToken> findAllByUserAndStatus(User user, RefreshToken.Status status);

	void deleteByUser(User user);
}
