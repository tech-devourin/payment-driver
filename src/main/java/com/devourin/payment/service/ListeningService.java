package com.devourin.payment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.devourin.payment.exception.DataException;
import com.devourin.payment.exception.PortInUseException;
import com.devourin.payment.model.DevicePortMapping;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.util.Rs232Util;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@Service
public class ListeningService implements SerialPortDataListener {

	@Autowired
	NetsRs232Service netsRs232Service;

	//	@Autowired
	//	NetsEthernetService netsEthernetService;

	@Autowired
	PaymentMessagingService paymentMessagingService;

	@Autowired
	Map<String, DevicePortMapping> openPorts;

	private static Logger logger = LoggerFactory.getLogger(ListeningService.class);

	@SuppressWarnings("java:S116")
	private boolean alreadyReading_Rs232 = false;

	// For RS232
	@Override
	public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }

	// For RS232
	@Override
	@Async
	public void serialEvent(SerialPortEvent event) {
		if (alreadyReading_Rs232 || event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
			return;
		}

		alreadyReading_Rs232 = true;

		SerialPort port = event.getSerialPort();

		ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

		try {
			Rs232Util.receiveData(byteArrayOS, port, Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			paymentMessagingService.sendWebsocketError(e);
			alreadyReading_Rs232 = false;
			Thread.currentThread().interrupt();
			return;
		}
		// Cannot use `finally`, as the thread is being interrupted
		alreadyReading_Rs232 = false;

		byte[] newData = byteArrayOS.toByteArray();

		if(isBlank(newData)) {
			return;
		}

		if(logger.isInfoEnabled()) {
			logger.info(String.format("Received: %s", Rs232Util.getHexString(newData)));
		}

		port.flushIOBuffers();
		PaymentDevice paymentDevice = openPorts.get(port.getSystemPortName()).getPaymentDevice();

		// Since this function deals with SerialPorts, it is always the RS232 version
		try {
			switch(paymentDevice.getModel()) {
			case NETS:
				netsRs232Service.receiveMessage(this, newData, port, paymentDevice);
				break;
			default:
				break;

			}
		} catch(DataException e) {
			paymentMessagingService.sendWebsocketDataError(e);
		} catch (SerialPortInvalidPortException | PortInUseException | IOException e) {
			paymentMessagingService.sendWebsocketError(e);
		}

	}

	// Checks if the entire message is 0's
	private boolean isBlank(byte[] message) {
		for(int i = 0; i < message.length; i++) {
			if(message[i] != 0) {
				return false;
			}
		}

		return true;
	}
}
