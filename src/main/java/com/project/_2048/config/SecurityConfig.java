package com.project._2048.config;

import com.project._2048.security.LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	private static final int REMEMBER_ME_SECONDS = 14 * 24 * 60 * 60;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, LoginSuccessHandler loginSuccessHandler,
			UserDetailsService userDetailsService,
			@Value("${app.security.remember-me-key}") String rememberMeKey) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/login", "/register", "/leaderboard", "/players/**",
								"/api/scores/top", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error")
						.permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.successHandler(loginSuccessHandler)
						.failureUrl("/login?error")
						.permitAll())
				.rememberMe(remember -> remember
						.key(rememberMeKey)
						.tokenValiditySeconds(REMEMBER_ME_SECONDS)
						.userDetailsService(userDetailsService))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout")
						.deleteCookies("JSESSIONID", "remember-me")
						.permitAll());
		return http.build();
	}
}
