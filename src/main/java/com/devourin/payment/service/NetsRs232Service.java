package com.devourin.payment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.devourin.payment.constant.Model;
import com.devourin.payment.constant.PaymentMethod;
import com.devourin.payment.exception.DataException;
import com.devourin.payment.exception.PortInUseException;
import com.devourin.payment.model.DevicePortMapping;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.model.PaymentInfo;
import com.devourin.payment.util.NumberUtil;
import com.devourin.payment.util.Rs232Util;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@Service
public class NetsRs232Service implements NetsService {

	@Autowired
	Map<Model, Map<String, Long>> paymentIdMap;

	@Autowired
	Map<String, DevicePortMapping> openPorts;

	@Autowired
	PaymentMessagingService paymentMessagingService;

	private final Predicate<byte[]> checkLrc = arr -> checkLrc(arr, 1);

	@Override
	public PaymentMessagingService getPaymentMessagingService() {
		return paymentMessagingService;
	}

	public void test(PaymentDevice paymentDevice, ListeningService listeningService) throws PortInUseException, SerialPortInvalidPortException, IOException {
		requestDeviceStatus(paymentDevice, listeningService);
	}

	@Override
	public void createPayment(PaymentInfo body, PaymentDevice paymentDevice, ListeningService listeningService) throws SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = getPort(paymentDevice, listeningService);

		Codes codes = getPaymentCodes(body.getPaymentMethod());

		byte[] message = concatByteArrays(
				createMessageHeader(codes.getFunction(), codes.getVersion(), paymentIdMap),
				createMessageData(FieldCode.TRANSACTION_AMOUNT, NumberUtil.padNumberWithZeroes(BigDecimal.valueOf(body.getAmount()), 12))
				);
		if(body.getPaymentMethod() == PaymentMethod.NETS) {
			message = concatByteArrays(message, createMessageData(FieldCode.TRANSACTION_TYPE_INDICATOR, "01"));
			
		}
		sendMessage(port, createMessage(message));
	}

	@Override
	public void requestDeviceStatus(PaymentDevice paymentDevice, ListeningService listeningService) throws SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = getPort(paymentDevice, listeningService);

		requestDeviceStatus(port);
	}

	@Override
	public void logonToDevice(PaymentDevice paymentDevice, ListeningService listeningService) throws SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = getPort(paymentDevice, listeningService);

		logonToDevice(port);
	}

	@Override
	public void getLastApprovedTransaction(PaymentDevice paymentDevice, ListeningService listeningService) throws SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = getPort(paymentDevice, listeningService);

		getLastApprovedTransaction(port);
	}

	@Override
	public void callTMS(PaymentDevice paymentDevice, ListeningService listeningService) throws SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = getPort(paymentDevice, listeningService);

		sendMessageOnlyHeader(FunctionCode.TMS, port);
	}

	@Override
	public void performSettlements(PaymentDevice paymentDevice, ListeningService listeningService) throws SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = getPort(paymentDevice, listeningService);

		performSettlement(port);
	}

	private void sendMessageOnlyHeader(String functionCode, SerialPort port) throws IOException {
		byte[] message = createMessage(createMessageHeader(functionCode, VersionCode.NETS, paymentIdMap));

		sendMessage(port, message);
	}

	private void sendMessage(SerialPort port, byte[] message) throws IOException {
		port.flushIOBuffers();
		Rs232Util.sendWithAckCheck(port, message, TOTAL_TRIES);
	}

	private void requestDeviceStatus(SerialPort port) throws IOException {
		sendMessageOnlyHeader(FunctionCode.REQUEST_TERMINAL_STATUS, port);
		// len 23
	}

	private void logonToDevice(SerialPort port) throws IOException {
		sendMessageOnlyHeader(FunctionCode.LOGON, port);
		// len 215
	}

	private void getLastApprovedTransaction(SerialPort port) throws IOException {
		sendMessageOnlyHeader(FunctionCode.GET_LAST_APPROVED_TRANSACTION, port);
	}

	private void performSettlement(SerialPort port) throws IOException {
		sendMessageOnlyHeader(FunctionCode.SETTLEMENT, port);
	}

	@Override
	public byte[] createMessage(byte[] body) {
		ByteArrayOutputStream message = new ByteArrayOutputStream(body.length + 5);

		message.write(Rs232Util.STX);

		writeLengthInBcd(message, body.length);

		// Add body
		message.write(body, 0, body.length);

		message.write(Rs232Util.ETX);

		// Setting LRC
		return addLrc(message, 1);  // Start from 1 because we do not count STX
	}

	@Override
	public byte[] generateHomogenisedMessage(byte[] message) {
		return Arrays.copyOfRange(message, 3, message.length - 1);
	}

	private SerialPort connectPort(String portName, ListeningService listeningService) throws PortInUseException, SerialPortInvalidPortException {
		return Rs232Util.connect(portName, 9600, 8, 1, SerialPort.NO_PARITY, 1800, listeningService, true, false);
	}

	public void receiveMessage(ListeningService listeningService, byte[] response, SerialPort port, PaymentDevice paymentDevice) throws DataException, SerialPortInvalidPortException, PortInUseException, IOException {
		/*
		 * The following code is to make sure that only one message is in a response.
		 * 
		 * 0 = haven't come across 0x03 yet
		 * 1 = come across 0x03
		 * 2 = come across the byte after 0x03 (LRC)
		 */
		int messageCheckValue = 0;

		for(int i = 0; i < response.length; i++) {
			switch(messageCheckValue) {
			case 0:
				if(response[i] == Rs232Util.ETX) {
					messageCheckValue = 1;
				}
				break;

			case 1:
				messageCheckValue = 2;
				break;

			case 2:
				response[i] = 0;
				break;

			default:
			}
		}

		response = Rs232Util.truncateMessage(response);

		boolean isCorrect = checkLrc.test(response);

		byte[] returnedValue = {isCorrect ? Rs232Util.ACK : Rs232Util.NACK};

		port.writeBytes(returnedValue, 1);

		if(isCorrect) {
			handleAsyncMessageHandling(listeningService, response, paymentIdMap, paymentDevice);
		}
	}

	SerialPort getPort(PaymentDevice paymentDevice, ListeningService listeningService) throws SerialPortInvalidPortException, PortInUseException {
		String portName = paymentDevice.getAddress();

		if(openPorts.containsKey(portName)) {
			SerialPort port = openPorts.get(portName).getSerialPort();
			if(port.isOpen()) {
				return port;
			}
		}

		// Come here if there is no port in the map, or if the port has closed

		SerialPort port = connectPort(portName, listeningService);
		openPorts.put(portName, new DevicePortMapping(paymentDevice, port));
		return port;
	}

}
