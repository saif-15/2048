package com.project._2048.controller;

import com.project._2048.dto.LeaderboardEntry;
import com.project._2048.dto.ScoreResult;
import com.project._2048.dto.ScoreSubmission;
import com.project._2048.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** JSON endpoints used by the in-browser game. */
@RestController
@RequestMapping("/api/scores")
public class ScoreApiController {

	private final GameService gameService;

	public ScoreApiController(GameService gameService) {
		this.gameService = gameService;
	}

	@PostMapping
	public ScoreResult submit(@Valid @RequestBody ScoreSubmission submission, Principal principal) {
		return gameService.submitScore(principal.getName(), submission);
	}

	@GetMapping("/top")
	public List<LeaderboardEntry> top(@RequestParam(defaultValue = "10") int limit) {
		return gameService.topPlayers(Math.max(1, Math.min(limit, 100)));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> invalidScore(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
	}
}
