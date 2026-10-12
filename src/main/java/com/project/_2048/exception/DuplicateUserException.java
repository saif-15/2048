package com.project._2048.exception;

import lombok.Getter;

/** Raised when a username or email is already registered. {@code field} names the offending form field. */
@Getter
public class DuplicateUserException extends RuntimeException {

	private final String field;

	public DuplicateUserException(String field, String message) {
		super(message);
		this.field = field;
	}
}
