package com.devourin.payment.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Predicate;

import org.slf4j.LoggerFactory;

import com.devourin.payment.exception.PortInUseException;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import org.slf4j.Logger;

public class Rs232Util {

	private Rs232Util() {}

	private static Logger logger = LoggerFactory.getLogger(Rs232Util.class);

	public static final byte ACK = 0x06;
	public static final byte NACK = 0x13;
	public static final byte STX = 0x02;
	public static final byte ETX = 0x03;

	private static final byte[] ACK_ARRAY = {ACK};
	private static final byte[] NACK_ARRAY = {NACK};

	public static SerialPort connect(String portName, int baudRate, int dataBits, int stopBits, int parity, SerialPortDataListener listener, boolean isReadBlocking, boolean isWriteBlocking) throws SerialPortInvalidPortException, PortInUseException {
		return connect(portName, baudRate, dataBits, stopBits, parity, -1, listener, isReadBlocking, isWriteBlocking);
	}

	public static SerialPort connect(String portName, int baudRate, int dataBits, int stopBits, int parity, int timeout, SerialPortDataListener listener, boolean isReadBlocking, boolean isWriteBlocking) throws SerialPortInvalidPortException, PortInUseException {
		SerialPort port = SerialPort.getCommPort(portName);
		if(port.isOpen()) {
			throw new PortInUseException("The port was already in use by another terminal.", null);
		}

		port.setComPortParameters(baudRate, dataBits, stopBits, parity);

		int blocking = isReadBlocking
				? SerialPort.TIMEOUT_READ_BLOCKING
				: SerialPort.TIMEOUT_READ_SEMI_BLOCKING;

		if(isWriteBlocking) {
			blocking |= SerialPort.TIMEOUT_WRITE_BLOCKING;
		}

		if(timeout < 0) { // timeout is 0 by default
			timeout = 0;
		}

		port.setComPortTimeouts(blocking , timeout, 0);

		if(listener != null) {
			port.addDataListener(listener);
		}

		port.openPort();

		return port;
	}

	public static void readWithAckCheck(SerialPort port, byte[] message, int totalTries, Predicate<byte[]> lrcCheck) throws InterruptedException {
		readMessage(port, message, totalTries, false, lrcCheck);
	}

	public static void readWithoutAckCheck(SerialPort port, byte[] message) throws InterruptedException {
		readMessage(port, message, 1, true, null); // Predicate will not be triggered, doesn't matter
	}

	private static void readMessage(SerialPort port, byte[] message, int totalTries, boolean skipCheck, Predicate<byte[]> lrcCheck) throws InterruptedException {

		if(!skipCheck && lrcCheck == null) {
			throw new IllegalArgumentException("No argument for the LRC checker was given, while the check was not skipped.", null);
		}

		for(int i = 0; i < totalTries; i++) {
			ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

			receiveData(byteArrayOS, port, message.length);

			byte[] receivedData = byteArrayOS.toByteArray();

			System.arraycopy(receivedData, 0, message, 0, receivedData.length);

			if(logger.isInfoEnabled()) {
				logger.info(String.format("Received: %s", getHexString(message)));
			}

			// If the message should not be checked, return
			if(skipCheck) {
				return;
			}

			// If the check passes, return
			if(lrcCheck.test(message)) {
				port.writeBytes(ACK_ARRAY, 1);
				return;
			}

			port.writeBytes(NACK_ARRAY, 1);
			message = new byte[message.length];
		}
	}

	public static void sendWithAckCheck(SerialPort port, byte[] message, int totalTries) throws IOException {
		sendMessage(port, message, totalTries, false);
	}

	public static void sendWithoutAckCheck(SerialPort port, byte[] message, int totalTries) throws IOException {
		sendMessage(port, message, totalTries, true);
	}

	private static void sendMessage(SerialPort port, byte[] message, int totalTries, boolean skipCheck) throws IOException {
	
		if(!port.isOpen()) {
			throw new IOException("The port is closed");
		}

		byte[] check = {0x00};
		port.flushIOBuffers();

		for(int i = 0; i < totalTries; i++) {
			port.writeBytes(message, message.length);

			if(logger.isInfoEnabled()) {
				logger.info(String.format("Sent: %s", getHexString(message)));
			}

			if(skipCheck) {
				port.flushIOBuffers();
				return;
			}

			port.readBytes(check, 1);
			if(check[0] == ACK) {
				port.flushIOBuffers();
				return;
			}
		}

		// Only come here if the check value was not ACK (0x06) at any point
		throw new IOException("The recipient did not receive a proper message in the total number of tries.");
	}

	public static void receiveData(ByteArrayOutputStream byteArrayOS, SerialPort port, int maxSize) throws InterruptedException {
		int availableBytes = 0;

		double ipg = 320_000d / port.getBaudRate(); // interpacket gap, in milliseconds
		int millis = (int) Math.floor(ipg);
		int nanos = (int) Math.floor((ipg - millis) * 1000);

		do {
			byte[] buffer = new byte[port.bytesAvailable()];
			port.readBytes(buffer, availableBytes);

			if(byteArrayOS.size() + availableBytes > maxSize) {
				int bytesLeft = maxSize - byteArrayOS.size();
				byteArrayOS.writeBytes(Arrays.copyOfRange(buffer, 0, bytesLeft));
			}

			byteArrayOS.writeBytes(buffer);

			Thread.sleep(millis, nanos);

			availableBytes = port.bytesAvailable();
		} while(availableBytes > 0);
	}

	public static byte[] truncateMessage(byte[] message) {
		int end;
		byte[] truncatedArray = new byte[0]; // Value if the loop's `if` is never true
		int start;

		for(start = 0; start < message.length; start++) {
			if(message[start] == Rs232Util.STX) {
				break;
			}
		}

		for(end = message.length - 1; end >= 0; end--) {
			if(message[end] != 0x00) {
				truncatedArray = Arrays.copyOfRange(message, start, end + 1);
				break;
			}
		}

		return truncatedArray;
	}

	public static String getHexString(byte... message) {
		StringBuilder sb = new StringBuilder();
		for(byte b : message) {

			if(b < 16 && b >= 0) sb.append("0");

			sb.append(String.format("%x ", b));
		}
		return sb.toString().stripTrailing();
	}
}
