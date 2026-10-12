package com.project._2048.controller;

import com.project._2048.model.User;
import com.project._2048.service.GameService;
import com.project._2048.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class GameController {

	private static final int LEADERBOARD_PAGE_SIZE = 20;

	private final GameService gameService;
	private final UserService userService;

	public GameController(GameService gameService, UserService userService) {
		this.gameService = gameService;
		this.userService = userService;
	}

	@GetMapping("/play")
	public String play(Principal principal, Model model) {
		User user = userService.getByUsername(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("rank", gameService.rankOf(user));
		model.addAttribute("globalBest", gameService.globalBestScore());
		model.addAttribute("recentGames", gameService.recentGames(user));
		return "game/play";
	}

	@GetMapping("/leaderboard")
	public String leaderboard(@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("players", gameService.leaderboard(Math.max(page, 0), LEADERBOARD_PAGE_SIZE));
		model.addAttribute("bestGames", gameService.bestGamesGlobal(10));
		return "leaderboard";
	}
}
