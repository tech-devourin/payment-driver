package com.devourin.payment.bean;

import java.util.List;

import com.devourin.payment.model.PaymentDevice;

public class PaymentDeviceList {
	private final List<PaymentDevice> list;

	public PaymentDeviceList(List<PaymentDevice> list) {
		this.list = list;
	}

	public List<PaymentDevice> getList() {
		return list;
	}
}
