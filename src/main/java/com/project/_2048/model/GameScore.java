package com.project._2048.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One finished game of 2048. */
@Entity
@Table(name = "game_scores", indexes = {
		@Index(name = "idx_scores_score", columnList = "score"),
		@Index(name = "idx_scores_user_played", columnList = "user_id, played_at")
})
@Getter
@Setter
@NoArgsConstructor
public class GameScore {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private int score;

	@Column(name = "highest_tile", nullable = false)
	private int highestTile;

	@Column(nullable = false)
	private int moves;

	@Column(name = "duration_seconds", nullable = false)
	private int durationSeconds;

	@Column(nullable = false)
	private boolean won;

	@Column(name = "played_at", nullable = false)
	private LocalDateTime playedAt;

	@PrePersist
	void onCreate() {
		if (playedAt == null) {
			playedAt = LocalDateTime.now();
		}
	}

	public String getDurationFormatted() {
		return "%d:%02d".formatted(durationSeconds / 60, durationSeconds % 60);
	}
}
