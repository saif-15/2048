package com.project._2048.controller;

import com.project._2048.model.Role;
import com.project._2048.model.User;
import com.project._2048.service.GameService;
import com.project._2048.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

	private static final int PAGE_SIZE = 20;

	private final UserService userService;
	private final GameService gameService;

	public AdminController(UserService userService, GameService gameService) {
		this.userService = userService;
		this.gameService = gameService;
	}

	@GetMapping
	public String dashboard(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
			Model model) {
		PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt")));
		model.addAttribute("users", userService.search(q, pageable));
		model.addAttribute("q", q);
		model.addAttribute("totalPlayers", gameService.totalPlayers());
		model.addAttribute("totalGames", gameService.totalGames());
		model.addAttribute("gamesToday", gameService.gamesToday());
		model.addAttribute("globalBest", gameService.globalBestScore());
		return "admin/dashboard";
	}

	@PostMapping("/users/{id}/toggle")
	public String toggleEnabled(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
		return act(redirect, () -> {
			User u = userService.toggleEnabled(id, principal.getName());
			return "%s is now %s.".formatted(u.getUsername(), u.isEnabled() ? "enabled" : "disabled");
		});
	}

	@PostMapping("/users/{id}/role")
	public String changeRole(@PathVariable Long id, @RequestParam Role role, Principal principal,
			RedirectAttributes redirect) {
		return act(redirect, () -> {
			User u = userService.changeRole(id, role, principal.getName());
			return "%s is now %s.".formatted(u.getUsername(), u.getRole());
		});
	}

	@PostMapping("/users/{id}/delete")
	public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
		return act(redirect, () -> {
			String name = userService.getById(id).getUsername();
			userService.delete(id, principal.getName());
			return "Deleted %s and all their games.".formatted(name);
		});
	}

	private static String act(RedirectAttributes redirect, java.util.function.Supplier<String> action) {
		try {
			redirect.addFlashAttribute("success", action.get());
		}
		catch (IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin";
	}
}
