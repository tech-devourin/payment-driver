package com.devourin.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.devourin.payment.exception.DataException;
import com.devourin.payment.model.Message;
import com.devourin.payment.util.ExceptionUtil;

@Service
public class PaymentMessagingService {

	@Autowired
	SimpMessagingTemplate simpMessagingTemplate;

	private static final String WEBSOCKET_ENDPOINT = "/queue/payment";

	public void sendWebsocketMessage(Message<?> payload) {
		simpMessagingTemplate.convertAndSend(WEBSOCKET_ENDPOINT, payload);
	}
	public void sendWebsocketError(Exception e) {
		sendWebsocketMessage(ExceptionUtil.getMessage(e));
	}
	public void sendWebsocketDataError(DataException e) {
		sendWebsocketMessage(Message.error(e.toMap()));
	}
	public void sendWebsocketSuccess() {
		sendWebsocketMessage(Message.success("Payment Successfull"));
	}
}
