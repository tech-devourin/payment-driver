package com.devourin.payment.constant;

public class WebsocketStatus {
	// Status codes from 4000-4999 are for private use
	private WebsocketStatus() {}

	public static final int SUCCESS = 4000;
	public static final int BAD_REQUEST = 4001;
	public static final int RESEND_MESSAGE = 4002;
}
