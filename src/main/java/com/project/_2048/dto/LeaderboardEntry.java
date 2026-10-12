package com.project._2048.dto;

/** Public leaderboard row exposed through the JSON API. */
public record LeaderboardEntry(
		long rank,
		String username,
		String displayName,
		int highestScore,
		int highestTile,
		int gamesPlayed) {
}
