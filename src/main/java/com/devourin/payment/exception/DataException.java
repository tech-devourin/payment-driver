package com.devourin.payment.exception;

import java.util.HashMap;
import java.util.Map;

public class DataException extends Exception {

	private static final long serialVersionUID = 9170999793382310074L;

	String error;

	public DataException(String message) {
		super(message, null);
	}

	public DataException(String message, String error) {
		super(message, null);

		this.error = error;
	}

	public String getError() {
		return error;
	}

	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<>();

		map.put("error", error != null ? error : "Data Exception");

		if(this.getMessage() != null) {
			map.put("message", this.getMessage());
		}

		return map;
	}

}
