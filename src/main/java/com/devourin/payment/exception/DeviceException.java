package com.devourin.payment.exception;

public class DeviceException extends DevourinException {
	private static final long serialVersionUID = -6736656052890744814L;

	public DeviceException() { super(); }

	public DeviceException(String message, Throwable cause) {
		super(message, cause);
	}

	public DeviceException(String message) {
		super(message);
	}

	public DeviceException(String error, String message) {
		super(error, message);
	}
}
