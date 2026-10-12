package com.project._2048.service;

import com.project._2048.dto.LeaderboardEntry;
import com.project._2048.dto.PlayerStats;
import com.project._2048.dto.ScoreResult;
import com.project._2048.dto.ScoreSubmission;
import com.project._2048.exception.ResourceNotFoundException;
import com.project._2048.model.GameScore;
import com.project._2048.model.User;
import com.project._2048.repository.GameScoreRepository;
import com.project._2048.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class GameService {

	public static final Sort LEADERBOARD_SORT = Sort.by(
			Sort.Order.desc("highestScore"),
			Sort.Order.desc("highestTile"),
			Sort.Order.asc("username"));

	private final UserRepository userRepository;
	private final GameScoreRepository gameScoreRepository;

	public GameService(UserRepository userRepository, GameScoreRepository gameScoreRepository) {
		this.userRepository = userRepository;
		this.gameScoreRepository = gameScoreRepository;
	}

	/**
	 * Persists a finished game and updates the player's aggregate statistics.
	 *
	 * @throws IllegalArgumentException if the submission is not a plausible 2048 result
	 */
	@Transactional
	public ScoreResult submitScore(String username, ScoreSubmission submission) {
		validate(submission);
		User user = userRepository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new ResourceNotFoundException("Player '" + username + "' not found"));

		GameScore game = new GameScore();
		game.setUser(user);
		game.setScore(submission.score());
		game.setHighestTile(submission.highestTile());
		game.setMoves(submission.moves());
		game.setDurationSeconds(submission.durationSeconds());
		game.setWon(submission.won());
		game.setPlayedAt(LocalDateTime.now());
		gameScoreRepository.save(game);

		boolean newHighScore = submission.score() > user.getHighestScore();
		if (newHighScore) {
			user.setHighestScore(submission.score());
		}
		user.setHighestTile(Math.max(user.getHighestTile(), submission.highestTile()));
		user.setGamesPlayed(user.getGamesPlayed() + 1);
		if (submission.won()) {
			user.setGamesWon(user.getGamesWon() + 1);
		}
		user.setTotalScore(user.getTotalScore() + submission.score());
		user.setTotalMoves(user.getTotalMoves() + submission.moves());
		user.setLastPlayedAt(game.getPlayedAt());
		userRepository.flush();

		long rank = userRepository.countByHighestScoreGreaterThan(user.getHighestScore()) + 1;
		return new ScoreResult(game.getId(), game.getScore(), newHighScore, user.getHighestScore(), rank,
				user.getGamesPlayed());
	}

	/**
	 * Sanity checks that cannot be expressed with bean validation. The game runs in the browser,
	 * so these only reject impossible values; they are not a full anti-cheat.
	 */
	static void validate(ScoreSubmission s) {
		int tile = s.highestTile();
		if (Integer.bitCount(tile) != 1) {
			throw new IllegalArgumentException("Highest tile must be a power of two");
		}
		if (s.score() % 2 != 0) {
			throw new IllegalArgumentException("Score must be even");
		}
		if (s.won() && tile < 2048) {
			throw new IllegalArgumentException("A won game must contain the 2048 tile");
		}
		// Building a tile of value 2^n by merging alone yields at most (n-1) * 2^n points,
		// so the score can never exceed that bound times the 16 board cells.
		int n = Integer.numberOfTrailingZeros(tile);
		long maxScore = 16L * Math.max(n - 1, 0) * tile;
		if (s.score() > maxScore) {
			throw new IllegalArgumentException("Score is too high for the highest tile reached");
		}
	}

	public Page<User> leaderboard(int page, int size) {
		return userRepository.findByGamesPlayedGreaterThan(0, PageRequest.of(page, size, LEADERBOARD_SORT));
	}

	public List<LeaderboardEntry> topPlayers(int limit) {
		AtomicLong rank = new AtomicLong();
		return leaderboard(0, limit).stream()
				.map(u -> new LeaderboardEntry(rank.incrementAndGet(), u.getUsername(), u.getNameToShow(),
						u.getHighestScore(), u.getHighestTile(), u.getGamesPlayed()))
				.toList();
	}

	public Page<GameScore> history(User user, int page, int size) {
		return gameScoreRepository.findByUserOrderByPlayedAtDesc(user, PageRequest.of(page, size));
	}

	public List<GameScore> recentGames(User user) {
		return gameScoreRepository.findTop5ByUserOrderByPlayedAtDesc(user);
	}

	public List<GameScore> recentGamesGlobal(int limit) {
		return gameScoreRepository.findRecentWithUser(Pageable.ofSize(limit));
	}

	public List<GameScore> bestGamesGlobal(int limit) {
		return gameScoreRepository.findTopGamesWithUser(Pageable.ofSize(limit));
	}

	public int globalBestScore() {
		return userRepository.findGlobalBestScore();
	}

	public long totalGames() {
		return gameScoreRepository.count();
	}

	public long totalPlayers() {
		return userRepository.count();
	}

	public long gamesToday() {
		return gameScoreRepository.countByPlayedAtAfter(LocalDate.now().atStartOfDay());
	}

	public Long rankOf(User user) {
		if (user.getGamesPlayed() == 0) {
			return null;
		}
		return userRepository.countByHighestScoreGreaterThan(user.getHighestScore()) + 1;
	}

	public PlayerStats statsFor(User user) {
		Double avg = gameScoreRepository.findAverageScore(user);
		List<PlayerStats.TileCount> tiles = gameScoreRepository.findTileDistribution(user).stream()
				.map(row -> new PlayerStats.TileCount(((Number) row[0]).intValue(), ((Number) row[1]).longValue()))
				.toList();
		return new PlayerStats(
				user.getGamesPlayed(),
				user.getGamesWon(),
				user.getHighestScore(),
				user.getHighestTile(),
				avg == null ? 0 : Math.round(avg),
				user.getTotalScore(),
				user.getTotalMoves(),
				rankOf(user),
				tiles);
	}
}
