package com.devourin.payment.exception;

public class CouldNotProcessException extends Exception {
	private static final long serialVersionUID = -2576760769605028956L;

	public CouldNotProcessException(String message) {
		super(message);
	}

	public CouldNotProcessException(String message, Throwable cause) {
		super(message, cause);
	}
}
