package com.project._2048.service;

import com.project._2048.dto.PasswordChangeForm;
import com.project._2048.dto.ProfileForm;
import com.project._2048.dto.RegistrationForm;
import com.project._2048.exception.DuplicateUserException;
import com.project._2048.exception.ResourceNotFoundException;
import com.project._2048.model.Role;
import com.project._2048.model.User;
import com.project._2048.repository.GameScoreRepository;
import com.project._2048.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final GameScoreRepository gameScoreRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, GameScoreRepository gameScoreRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.gameScoreRepository = gameScoreRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User register(RegistrationForm form) {
		String username = form.getUsername().trim();
		String email = normalizeEmail(form.getEmail());
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new DuplicateUserException("username", "That username is already taken");
		}
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateUserException("email", "An account with this email already exists");
		}
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setDisplayName(blankToNull(form.getDisplayName()));
		user.setCountry(blankToNull(form.getCountry()));
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setRole(Role.USER);
		return userRepository.save(user);
	}

	public User getByUsername(String username) {
		return userRepository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new ResourceNotFoundException("Player '" + username + "' not found"));
	}

	public User getById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User #" + id + " not found"));
	}

	@Transactional
	public void recordLogin(String username) {
		userRepository.findByUsernameIgnoreCase(username).ifPresent(u -> u.setLastLoginAt(LocalDateTime.now()));
	}

	@Transactional
	public void updateProfile(String username, ProfileForm form) {
		User user = getByUsername(username);
		String email = normalizeEmail(form.getEmail());
		boolean emailTaken = userRepository.findByEmailIgnoreCase(email)
				.filter(other -> !other.getId().equals(user.getId()))
				.isPresent();
		if (emailTaken) {
			throw new DuplicateUserException("email", "An account with this email already exists");
		}
		user.setEmail(email);
		user.setDisplayName(blankToNull(form.getDisplayName()));
		user.setCountry(blankToNull(form.getCountry()));
	}

	/** @return false if the current password did not match */
	@Transactional
	public boolean changePassword(String username, PasswordChangeForm form) {
		User user = getByUsername(username);
		if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
			return false;
		}
		user.setPassword(passwordEncoder.encode(form.getNewPassword()));
		return true;
	}

	// ---- administration -------------------------------------------------

	public Page<User> search(String query, Pageable pageable) {
		if (query == null || query.isBlank()) {
			return userRepository.findAll(pageable);
		}
		String q = query.trim();
		return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable);
	}

	@Transactional
	public User toggleEnabled(Long id, String actingAdmin) {
		User user = getById(id);
		requireNotSelf(user, actingAdmin);
		user.setEnabled(!user.isEnabled());
		return user;
	}

	@Transactional
	public User changeRole(Long id, Role role, String actingAdmin) {
		User user = getById(id);
		requireNotSelf(user, actingAdmin);
		user.setRole(role);
		return user;
	}

	@Transactional
	public void delete(Long id, String actingAdmin) {
		User user = getById(id);
		requireNotSelf(user, actingAdmin);
		gameScoreRepository.deleteAllByUser(user);
		userRepository.delete(user);
	}

	/** Creates the bootstrap administrator account if no user with that name exists yet. */
	@Transactional
	public boolean ensureAdmin(String username, String email, String rawPassword) {
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			return false;
		}
		User admin = new User();
		admin.setUsername(username);
		admin.setEmail(normalizeEmail(email));
		admin.setDisplayName("Administrator");
		admin.setPassword(passwordEncoder.encode(rawPassword));
		admin.setRole(Role.ADMIN);
		userRepository.save(admin);
		return true;
	}

	private static void requireNotSelf(User user, String actingAdmin) {
		if (user.getUsername().equalsIgnoreCase(actingAdmin)) {
			throw new IllegalStateException("You cannot change or delete your own admin account here");
		}
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
