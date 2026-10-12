package com.project._2048.dto;

import lombok.Value;

import java.util.List;

/** Aggregated statistics shown on a player's profile page. */
@Value
public class PlayerStats {

	int gamesPlayed;
	int gamesWon;
	int highestScore;
	int highestTile;
	long averageScore;
	long totalScore;
	long totalMoves;
	/** 1-based global rank, or null when the player has not finished a game yet. */
	Long rank;
	List<TileCount> tileDistribution;

	public int getWinRate() {
		return gamesPlayed == 0 ? 0 : (int) Math.round(gamesWon * 100.0 / gamesPlayed);
	}

	public long getMaxTileCount() {
		return tileDistribution.stream().mapToLong(TileCount::getGames).max().orElse(1);
	}

	@Value
	public static class TileCount {
		int tile;
		long games;
	}
}
