package com.devourin.payment.dto;

import com.devourin.payment.model.PaymentInfo;

public class PaymentInfoDto {
	private Long amount;
	private String paymentMethod;

	public PaymentInfoDto() {}

	public PaymentInfoDto(PaymentInfo paymentInfo) {
		this.amount = paymentInfo.getAmount();
		this.paymentMethod = paymentInfo.getPaymentMethod().name();
	}

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
}
