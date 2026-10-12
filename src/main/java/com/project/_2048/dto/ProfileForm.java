package com.project._2048.dto;

import com.project._2048.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileForm {

	@Size(max = 50, message = "Display name can be at most 50 characters")
	private String displayName;

	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email address")
	@Size(max = 100)
	private String email;

	@Size(max = 60)
	private String country;

	public static ProfileForm from(User user) {
		ProfileForm form = new ProfileForm();
		form.setDisplayName(user.getDisplayName());
		form.setEmail(user.getEmail());
		form.setCountry(user.getCountry());
		return form;
	}
}
