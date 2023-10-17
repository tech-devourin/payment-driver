package com.devourin.payment.exception;

import java.util.HashMap;
import java.util.Map;

public class DevourinException extends Exception {
	private static final long serialVersionUID = 7822581201545130574L;

	final String error;

	public DevourinException() { this.error = null;}

	public DevourinException(String message, Throwable cause) {
		super(message, cause);
		this.error = null;
	}

	public DevourinException(String message) {
		super(message, null);
		this.error = null;
	}

	public DevourinException(String error, String message) {
		super(message, null);

		this.error = error;
	}

	public String getError() {
		return error;
	}

	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<>();

		map.put("error", error != null ? error : this.getClass().getSimpleName());

		if(this.getMessage() != null) {
			map.put("message", this.getMessage());
		}

		return map;
	}
}
