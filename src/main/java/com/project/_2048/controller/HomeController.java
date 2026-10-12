package com.project._2048.controller;

import com.project._2048.service.GameService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	private final GameService gameService;

	public HomeController(GameService gameService) {
		this.gameService = gameService;
	}

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("topPlayers", gameService.leaderboard(0, 5).getContent());
		model.addAttribute("recentGames", gameService.recentGamesGlobal(8));
		model.addAttribute("totalPlayers", gameService.totalPlayers());
		model.addAttribute("totalGames", gameService.totalGames());
		model.addAttribute("globalBest", gameService.globalBestScore());
		return "index";
	}
}
