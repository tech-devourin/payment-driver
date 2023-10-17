package com.devourin.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.devourin.payment.exception.DevourinException;
import com.devourin.payment.model.Message;
import com.devourin.payment.model.ReturnedPaymentInfo;
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
	public void sendWebsocketDevourinError(DevourinException e) {
		sendWebsocketMessage(Message.error(e.toMap()));
	}
	public void sendWebsocketSuccess(ReturnedPaymentInfo paymentSuccess) {
		sendWebsocketMessage(Message.success(paymentSuccess));
	}
	public void sendWebsocketResendMessage(Exception e) {
		sendWebsocketMessage(Message.resendMessage(ExceptionUtil.getMap(e)));
	}
	public void sendWebsocketDevourinResendMessage(DevourinException e) {
		sendWebsocketMessage(Message.resendMessage(e.toMap()));
	}
}
