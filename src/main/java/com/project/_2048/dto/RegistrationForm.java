package com.project._2048.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationForm {

	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 30, message = "Username must be 3-30 characters")
	@Pattern(regexp = "^[A-Za-z0-9_]*$", message = "Only letters, numbers and underscores")
	private String username;

	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email address")
	@Size(max = 100)
	private String email;

	@Size(max = 50, message = "Display name can be at most 50 characters")
	private String displayName;

	@Size(max = 60)
	private String country;

	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 100, message = "Password must be at least 8 characters")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Password must contain a letter and a digit")
	private String password;

	@NotBlank(message = "Please confirm your password")
	private String confirmPassword;
}
