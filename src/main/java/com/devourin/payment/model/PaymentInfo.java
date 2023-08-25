package com.devourin.payment.model;

import java.util.List;

public class PaymentInfo {
	private Long amount;
	private String paymentMethod;
	private List<PaymentDevice> devices;

	public Long getAmount() {
		return amount;
	}
	public void setAmount(Long amount) {
		this.amount = amount;
	}
	public String getPaymentMethod() {
		return paymentMethod;
	}
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	public List<PaymentDevice> getDevices() {
		return devices;
	}
	public void setDevices(List<PaymentDevice> devices) {
		this.devices = devices;
	}
}
