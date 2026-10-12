package com.project._2048.controller;

import com.project._2048.dto.PasswordChangeForm;
import com.project._2048.dto.ProfileForm;
import com.project._2048.exception.DuplicateUserException;
import com.project._2048.model.User;
import com.project._2048.service.GameService;
import com.project._2048.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Objects;

@Controller
public class ProfileController {

	private static final int HISTORY_PAGE_SIZE = 10;

	private final UserService userService;
	private final GameService gameService;

	public ProfileController(UserService userService, GameService gameService) {
		this.userService = userService;
		this.gameService = gameService;
	}

	@GetMapping("/profile")
	public String myProfile(Principal principal, @RequestParam(defaultValue = "0") int page, Model model) {
		return renderProfile(userService.getByUsername(principal.getName()), true, "/profile", page, model);
	}

	@GetMapping("/players/{username}")
	public String playerProfile(@PathVariable String username, @RequestParam(defaultValue = "0") int page,
			Principal principal, Model model) {
		User user = userService.getByUsername(username);
		boolean owner = principal != null && principal.getName().equalsIgnoreCase(user.getUsername());
		return renderProfile(user, owner, "/players/" + user.getUsername(), page, model);
	}

	private String renderProfile(User user, boolean owner, String baseUrl, int page, Model model) {
		model.addAttribute("player", user);
		model.addAttribute("owner", owner);
		model.addAttribute("baseUrl", baseUrl);
		model.addAttribute("stats", gameService.statsFor(user));
		model.addAttribute("history", gameService.history(user, Math.max(page, 0), HISTORY_PAGE_SIZE));
		return "profile/view";
	}

	@GetMapping("/profile/edit")
	public String editForm(Principal principal, Model model) {
		User user = userService.getByUsername(principal.getName());
		model.addAttribute("profileForm", ProfileForm.from(user));
		model.addAttribute("passwordForm", new PasswordChangeForm());
		model.addAttribute("player", user);
		return "profile/edit";
	}

	@PostMapping("/profile/edit")
	public String updateProfile(Principal principal, @Valid @ModelAttribute("profileForm") ProfileForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				userService.updateProfile(principal.getName(), form);
				redirect.addFlashAttribute("success", "Profile updated.");
				return "redirect:/profile/edit";
			}
			catch (DuplicateUserException e) {
				result.rejectValue(e.getField(), "duplicate", e.getMessage());
			}
		}
		model.addAttribute("passwordForm", new PasswordChangeForm());
		model.addAttribute("player", userService.getByUsername(principal.getName()));
		return "profile/edit";
	}

	@PostMapping("/profile/password")
	public String changePassword(Principal principal, @Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (!result.hasFieldErrors("confirmPassword")
				&& !Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
		}
		if (!result.hasErrors()) {
			if (userService.changePassword(principal.getName(), form)) {
				redirect.addFlashAttribute("success", "Password changed.");
				return "redirect:/profile/edit";
			}
			result.rejectValue("currentPassword", "invalid", "Current password is incorrect");
		}
		User user = userService.getByUsername(principal.getName());
		model.addAttribute("profileForm", ProfileForm.from(user));
		model.addAttribute("player", user);
		return "profile/edit";
	}
}
