package com.project._2048.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the first administrator on startup so the admin panel is reachable. */
@Component
public class AdminInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

	private final UserService userService;
	private final String username;
	private final String email;
	private final String password;

	public AdminInitializer(UserService userService,
			@Value("${app.admin.username}") String username,
			@Value("${app.admin.email}") String email,
			@Value("${app.admin.password}") String password) {
		this.userService = userService;
		this.username = username;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (username.isBlank() || password.isBlank()) {
			return;
		}
		if (userService.ensureAdmin(username, email, password)) {
			log.warn("Created administrator account '{}'. Change its password after first login.", username);
		}
	}
}
