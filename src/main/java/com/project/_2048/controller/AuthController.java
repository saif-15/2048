package com.project._2048.controller;

import com.project._2048.dto.RegistrationForm;
import com.project._2048.exception.DuplicateUserException;
import com.project._2048.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/login")
	public String login(Authentication authentication) {
		return isLoggedIn(authentication) ? "redirect:/play" : "auth/login";
	}

	@GetMapping("/register")
	public String registerForm(Authentication authentication, Model model) {
		if (isLoggedIn(authentication)) {
			return "redirect:/play";
		}
		model.addAttribute("form", new RegistrationForm());
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result,
			RedirectAttributes redirect) {
		if (!result.hasFieldErrors("confirmPassword")
				&& !Objects.equals(form.getPassword(), form.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
		}
		if (result.hasErrors()) {
			return "auth/register";
		}
		try {
			userService.register(form);
		}
		catch (DuplicateUserException e) {
			result.rejectValue(e.getField(), "duplicate", e.getMessage());
			return "auth/register";
		}
		redirect.addFlashAttribute("success", "Account created! Sign in to start playing.");
		redirect.addFlashAttribute("username", form.getUsername().trim());
		return "redirect:/login";
	}

	private static boolean isLoggedIn(Authentication authentication) {
		return authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
