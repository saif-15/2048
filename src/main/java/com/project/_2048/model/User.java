package com.project._2048.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A registered player. Aggregate statistics (best score, games played, ...) are
 * denormalised here so the leaderboard can be served from a single indexed table.
 */
@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_highest_score", columnList = "highest_score"))
@Getter
@Setter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String username;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(name = "display_name", length = 50)
	private String displayName;

	@Column(length = 60)
	private String country;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.USER;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "highest_score", nullable = false)
	private int highestScore;

	@Column(name = "highest_tile", nullable = false)
	private int highestTile;

	@Column(name = "games_played", nullable = false)
	private int gamesPlayed;

	@Column(name = "games_won", nullable = false)
	private int gamesWon;

	@Column(name = "total_score", nullable = false)
	private long totalScore;

	@Column(name = "total_moves", nullable = false)
	private long totalMoves;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "last_played_at")
	private LocalDateTime lastPlayedAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public String getNameToShow() {
		return displayName == null || displayName.isBlank() ? username : displayName;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}
}
