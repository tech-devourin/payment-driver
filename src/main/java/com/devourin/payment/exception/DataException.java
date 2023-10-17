package com.devourin.payment.exception;

public class DataException extends DevourinException {
	private static final long serialVersionUID = 9170999793382310074L;

	public DataException() { super(); }

	public DataException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataException(String message) {
		super(message);
	}

	public DataException(String error, String message) {
		super(error, message);
	}

}
