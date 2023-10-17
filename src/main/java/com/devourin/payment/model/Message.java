package com.devourin.payment.model;

import java.util.Map;

import com.devourin.payment.constant.WebsocketStatus;

public class Message<T> {
	private T data;
	private int status;
	private Map<String, String> error;

	public Message() {}

	private Message(T data, int status, Map<String, String> error) {
		this.data = data;
		this.status = status;
		this.error = error;
	}

	public static <T> Message<T> success(T data) {
		return new Message<>(data, WebsocketStatus.SUCCESS, null);
	}

	public static Message<Object> error(Map<String, String> error) {
		return new Message<>(null, WebsocketStatus.BAD_REQUEST, error);
	}

	public static <T> Message<T> error(Map<String, String> error, T data) {
		return new Message<>(data, WebsocketStatus.BAD_REQUEST, error);
	}

	public static Message<Object> resendMessage(Map<String, String> error) {
		return new Message<>(null, WebsocketStatus.RESEND_MESSAGE, error);
	}

	public static <T> Message<T> resendMessage(Map<String, String> error, T data) {
		return new Message<>(data, WebsocketStatus.RESEND_MESSAGE, error);
	}

	public T getData() {
		return data;
	}
	public void setData(T data) {
		this.data = data;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public Map<String, String> getError() {
		return error;
	}
	public void setError(Map<String, String> error) {
		this.error = error;
	}
}
