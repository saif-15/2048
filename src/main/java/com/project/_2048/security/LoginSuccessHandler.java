package com.project._2048.security;

import com.project._2048.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Stores the last-login timestamp, then continues to the originally requested page (or /play). */
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private final UserService userService;

	public LoginSuccessHandler(UserService userService) {
		this.userService = userService;
		setDefaultTargetUrl("/play");
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		userService.recordLogin(authentication.getName());
		super.onAuthenticationSuccess(request, response, authentication);
	}
}
