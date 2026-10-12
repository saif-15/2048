package com.project._2048.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** JSON body posted by the browser when a game finishes. */
public record ScoreSubmission(
		@Min(0) @Max(10_000_000) int score,
		@Min(2) @Max(131_072) int highestTile,
		@Min(1) @Max(1_000_000) int moves,
		@Min(0) @Max(604_800) int durationSeconds,
		boolean won) {
}
