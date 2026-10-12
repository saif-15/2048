package com.project._2048.security;

import com.project._2048.model.User;
import com.project._2048.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads login credentials from the {@code users} table. */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public DatabaseUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		User user = userRepository.findByUsernameIgnoreCase(username.trim())
				.orElseThrow(() -> new UsernameNotFoundException("Unknown user " + username));
		return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
				.password(user.getPassword())
				.roles(user.getRole().name())
				.disabled(!user.isEnabled())
				.build();
	}
}
