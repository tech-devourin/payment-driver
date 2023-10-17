package com.devourin.payment.exception;

public class NeedToRestartPaymentException extends DevourinException {
	private static final long serialVersionUID = 5921010850181280554L;

	public NeedToRestartPaymentException() { super(); }

	public NeedToRestartPaymentException(String message, Throwable cause) {
		super(message, cause);
	}

	public NeedToRestartPaymentException(String message) {
		super(message);
	}

	public NeedToRestartPaymentException(String error, String message) {
		super(error, message);
	}

}
