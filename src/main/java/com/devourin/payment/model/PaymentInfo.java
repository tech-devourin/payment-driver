package com.devourin.payment.model;

import com.devourin.payment.constant.PaymentMethod;

public class PaymentInfo {
	private Long amount;
	private PaymentMethod paymentMethod;

	public Long getAmount() {
		return amount;
	}
	public void setAmount(Long amount) {
		this.amount = amount;
	}
	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}
	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
}
