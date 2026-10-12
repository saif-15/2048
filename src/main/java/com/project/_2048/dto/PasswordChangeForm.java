package com.project._2048.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeForm {

	@NotBlank(message = "Current password is required")
	private String currentPassword;

	@NotBlank(message = "New password is required")
	@Size(min = 8, max = 100, message = "Password must be at least 8 characters")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Password must contain a letter and a digit")
	private String newPassword;

	@NotBlank(message = "Please confirm the new password")
	private String confirmPassword;
}
