package com.project._2048;

import com.project._2048.model.User;
import com.project._2048.repository.GameScoreRepository;
import com.project._2048.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class GameFlowIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GameScoreRepository gameScoreRepository;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private ResultActions register(String username, String email, String password, String confirm) throws Exception {
		return mvc.perform(post("/register").with(csrf())
				.param("username", username)
				.param("email", email)
				.param("displayName", "")
				.param("password", password)
				.param("confirmPassword", confirm));
	}

	private ResultActions submitScore(String username, String json) throws Exception {
		return mvc.perform(post("/api/scores").with(csrf()).with(user(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	@Test
	void signUpLoginAndSaveScores() throws Exception {
		register("alice", "Alice@Example.com", "secret123", "secret123")
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		User alice = userRepository.findByUsernameIgnoreCase("alice").orElseThrow();
		assertThat(alice.getEmail()).isEqualTo("alice@example.com");
		assertThat(alice.getPassword()).isNotEqualTo("secret123").startsWith("$2");

		mvc.perform(formLogin("/login").user("alice").password("secret123"))
				.andExpect(authenticated().withUsername("alice").withRoles("USER"))
				.andExpect(redirectedUrl("/play"));
		mvc.perform(formLogin("/login").user("alice").password("wrong-pass1"))
				.andExpect(unauthenticated());

		submitScore("alice", """
				{"score":1024,"highestTile":128,"moves":200,"durationSeconds":95,"won":false}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.newHighScore").value(true))
				.andExpect(jsonPath("$.highestScore").value(1024))
				.andExpect(jsonPath("$.gamesPlayed").value(1));

		submitScore("alice", """
				{"score":512,"highestTile":64,"moves":120,"durationSeconds":40,"won":false}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.newHighScore").value(false))
				.andExpect(jsonPath("$.highestScore").value(1024));

		alice = userRepository.findByUsernameIgnoreCase("alice").orElseThrow();
		assertThat(alice.getHighestScore()).isEqualTo(1024);
		assertThat(alice.getHighestTile()).isEqualTo(128);
		assertThat(alice.getGamesPlayed()).isEqualTo(2);
		assertThat(alice.getTotalScore()).isEqualTo(1536);
		assertThat(alice.getLastLoginAt()).isNotNull();
		assertThat(gameScoreRepository.findTop5ByUserOrderByPlayedAtDesc(alice)).hasSize(2);

		mvc.perform(get("/api/scores/top"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("alice"));
	}

	@Test
	void registrationValidation() throws Exception {
		register("bob", "bob@example.com", "password1", "password1").andExpect(status().is3xxRedirection());

		register("BOB", "other@example.com", "password1", "password1")
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("form", "username"));
		register("bobby", "BOB@example.com", "password1", "password1")
				.andExpect(model().attributeHasFieldErrors("form", "email"));
		register("carol", "carol@example.com", "password1", "password2")
				.andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));
		register("x", "not-an-email", "short", "short")
				.andExpect(model().attributeHasFieldErrors("form", "username", "email", "password"));
	}

	@Test
	void implausibleScoresAreRejected() throws Exception {
		register("dave", "dave@example.com", "password1", "password1");

		submitScore("dave", """
				{"score":100,"highestTile":100,"moves":10,"durationSeconds":5,"won":false}""")
				.andExpect(status().isBadRequest());
		submitScore("dave", """
				{"score":999999,"highestTile":16,"moves":10,"durationSeconds":5,"won":false}""")
				.andExpect(status().isBadRequest());
		submitScore("dave", """
				{"score":2000,"highestTile":256,"moves":300,"durationSeconds":5,"won":true}""")
				.andExpect(status().isBadRequest());
		submitScore("dave", """
				{"score":-4,"highestTile":2,"moves":1,"durationSeconds":5,"won":false}""")
				.andExpect(status().isBadRequest());

		mvc.perform(post("/api/scores").with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"score\":4,\"highestTile\":4,\"moves\":1,\"durationSeconds\":1,\"won\":false}"))
				.andExpect(status().is3xxRedirection());
		mvc.perform(post("/api/scores").with(user("dave")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"score\":4,\"highestTile\":4,\"moves\":1,\"durationSeconds\":1,\"won\":false}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void pagesRenderAndAreProtected() throws Exception {
		register("erin", "erin@example.com", "password1", "password1");
		submitScore("erin", """
				{"score":2048,"highestTile":256,"moves":250,"durationSeconds":120,"won":false}""");

		for (String url : new String[] { "/", "/login", "/register", "/leaderboard", "/players/erin" }) {
			mvc.perform(get(url)).andExpect(status().isOk());
		}
		mvc.perform(get("/players/nobody-here")).andExpect(status().isNotFound());

		mvc.perform(get("/play")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
		mvc.perform(get("/play").with(user("erin"))).andExpect(status().isOk());
		mvc.perform(get("/profile").with(user("erin"))).andExpect(status().isOk());
		mvc.perform(get("/profile/edit").with(user("erin"))).andExpect(status().isOk());

		mvc.perform(get("/admin").with(user("erin"))).andExpect(status().isForbidden());
		mvc.perform(get("/admin").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
	}

	@Test
	void profileUpdateAndPasswordChange() throws Exception {
		register("frank", "frank@example.com", "password1", "password1");

		mvc.perform(post("/profile/edit").with(csrf()).with(user("frank"))
						.param("displayName", "Frankie").param("email", "frank2@example.com").param("country", "PK"))
				.andExpect(redirectedUrl("/profile/edit"));
		User frank = userRepository.findByUsernameIgnoreCase("frank").orElseThrow();
		assertThat(frank.getNameToShow()).isEqualTo("Frankie");
		assertThat(frank.getEmail()).isEqualTo("frank2@example.com");

		mvc.perform(post("/profile/password").with(csrf()).with(user("frank"))
						.param("currentPassword", "wrongpass1").param("newPassword", "newpass123")
						.param("confirmPassword", "newpass123"))
				.andExpect(model().attributeHasFieldErrors("passwordForm", "currentPassword"));
		mvc.perform(post("/profile/password").with(csrf()).with(user("frank"))
						.param("currentPassword", "password1").param("newPassword", "newpass123")
						.param("confirmPassword", "newpass123"))
				.andExpect(redirectedUrl("/profile/edit"));

		mvc.perform(formLogin("/login").user("frank").password("newpass123"))
				.andExpect(authenticated().withUsername("frank"));
	}

	@Test
	void adminCanDisableUser() throws Exception {
		register("gina", "gina@example.com", "password1", "password1");
		Long id = userRepository.findByUsernameIgnoreCase("gina").orElseThrow().getId();

		mvc.perform(post("/admin/users/{id}/toggle", id).with(csrf()).with(user("admin").roles("ADMIN")))
				.andExpect(redirectedUrl("/admin"));
		assertThat(userRepository.findById(id).orElseThrow().isEnabled()).isFalse();

		mvc.perform(formLogin("/login").user("gina").password("password1")).andExpect(unauthenticated());
	}
}
