package com.project._2048.dto;

/** Response returned after a score has been saved. */
public record ScoreResult(
		Long gameId,
		int score,
		boolean newHighScore,
		int highestScore,
		long rank,
		int gamesPlayed) {
}
