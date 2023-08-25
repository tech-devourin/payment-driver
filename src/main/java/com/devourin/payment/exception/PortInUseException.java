package com.devourin.payment.exception;

public class PortInUseException extends Exception {

	private static final long serialVersionUID = 5119380215859240473L;
	
	public PortInUseException(String message, Throwable cause) {
		super(message, cause);
	}

}
