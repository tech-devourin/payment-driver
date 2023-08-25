package com.devourin.payment.util;

public class EnvironmentUtil {

	public static String getTerminalId() {
		return System.getenv("DEVOURIN_TERMINAL");
	}
	
	public static String getServerUrl() {
		return System.getenv("DEVOURIN_SERVER_URL");
	}
}
