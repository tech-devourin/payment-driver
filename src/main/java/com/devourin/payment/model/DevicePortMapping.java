package com.devourin.payment.model;

import com.fazecast.jSerialComm.SerialPort;

public class DevicePortMapping {
	SerialPort serialPort;
	PaymentDevice paymentDevice;

	public DevicePortMapping() {}

	public DevicePortMapping(PaymentDevice paymentDevice, SerialPort serialPort) {
		this.serialPort = serialPort;
		this.paymentDevice = paymentDevice;
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}
	public void setSerialPort(SerialPort serialPort) {
		this.serialPort = serialPort;
	}
	public PaymentDevice getPaymentDevice() {
		return paymentDevice;
	}
	public void setPaymentDevice(PaymentDevice paymentDevice) {
		this.paymentDevice = paymentDevice;
	}
}
