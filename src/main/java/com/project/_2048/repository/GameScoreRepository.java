package com.project._2048.repository;

import com.project._2048.model.GameScore;
import com.project._2048.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GameScoreRepository extends JpaRepository<GameScore, Long> {

	Page<GameScore> findByUserOrderByPlayedAtDesc(User user, Pageable pageable);

	List<GameScore> findTop5ByUserOrderByPlayedAtDesc(User user);

	long countByPlayedAtAfter(LocalDateTime since);

	@Query("select avg(g.score) from GameScore g where g.user = :user")
	Double findAverageScore(@Param("user") User user);

	/** Rows of [highestTile, count] for the given player, smallest tile first. */
	@Query("select g.highestTile, count(g) from GameScore g where g.user = :user group by g.highestTile order by g.highestTile")
	List<Object[]> findTileDistribution(@Param("user") User user);

	@Query("select g from GameScore g join fetch g.user order by g.playedAt desc")
	List<GameScore> findRecentWithUser(Pageable pageable);

	@Query("select g from GameScore g join fetch g.user order by g.score desc, g.playedAt asc")
	List<GameScore> findTopGamesWithUser(Pageable pageable);

	@Modifying
	@Query("delete from GameScore g where g.user = :user")
	void deleteAllByUser(@Param("user") User user);
}
