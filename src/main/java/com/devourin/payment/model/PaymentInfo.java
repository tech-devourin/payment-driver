package com.devourin.payment.model;

import com.devourin.payment.constant.PaymentMethod;
import com.devourin.payment.dto.PaymentInfoDto;

public class PaymentInfo {
	private Long amount;
	private PaymentMethod paymentMethod;

	public PaymentInfo() {}

	public PaymentInfo(PaymentInfoDto paymentInfoDto) {
		this.paymentMethod = PaymentMethod.valueOf(paymentInfoDto.getPaymentMethod());
		this.amount = paymentInfoDto.getAmount();
	}

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
