package com.devourin.payment.model;

import com.devourin.payment.constant.Model;
import com.devourin.payment.constant.Protocol;

public class PaymentDevice {
	private Protocol protocol;
	private String address;
	private Model model;

	public Protocol getProtocol() {
		return protocol;
	}
	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public Model getModel() {
		return model;
	}
	public void setModel(Model model) {
		this.model = model;
	}
}
