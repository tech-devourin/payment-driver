package com.devourin.payment.util;

import java.util.Map;

import com.devourin.payment.model.Message;

public class ExceptionUtil {
	private ExceptionUtil() {}
	
	public static Map<String, String> getMap(Exception e) {
		return Map.of("error", e.getClass().toString(), "message", e.getMessage());
	}
	
	public static Message<Object> getMessage(Exception e) {
		return Message.error(getMap(e));
	}
}
