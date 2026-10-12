package com.project._2048.repository;

import com.project._2048.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsernameIgnoreCase(String username);

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);

	boolean existsByEmailIgnoreCase(String email);

	/** Leaderboard: only players that have finished at least one game. */
	Page<User> findByGamesPlayedGreaterThan(int gamesPlayed, Pageable pageable);

	long countByHighestScoreGreaterThan(int score);

	Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email, Pageable pageable);

	@Query("select coalesce(max(u.highestScore), 0) from User u")
	Integer findGlobalBestScore();
}
