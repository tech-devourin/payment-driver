package com.devourin.payment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import com.devourin.payment.exception.DataException;
import com.devourin.payment.exception.PortInUseException;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.model.PaymentInfo;
import com.devourin.payment.util.Rs232Util;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@Service
public class NetsRs232Service implements NetsService {

	private final Predicate<byte[]> checkLrc = (arr) -> checkLrc(arr, 1);

	public byte[] test(PaymentDevice paymentDevice) throws PortInUseException, SerialPortInvalidPortException, IOException {

		SerialPort port = connectPort(paymentDevice.getAddress());

		try {
			byte[] message = requestDeviceStatus(port);
			return message;
		} catch (Exception e) {
			throw e;
		} finally {
			port.closePort();
		}
	}

	@Override
	public void createPayment(PaymentInfo body, PaymentDevice paymentDevice) throws DataException, SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = connectPort(paymentDevice.getAddress());
		// If there is an error while opening the port, there is no need to close it
		try {
			byte[] resp = requestDeviceStatus(port);

			handleResponseExtended(resp);

			//			byte[] message = createRs232Message(new byte[1]); // TODO
			//			byte[] response = new byte[1]; // TODO: set response size
			//			
			//			
			//			Rs232Util.sendAndReceiveRs232MessageWithCheck(port, message, response, 3, checkLrcRs232);

			// TODO: check response and handle
		} catch(Exception e) {
			throw e;
		} finally {
			port.closePort();
		}

	}

	@Override
	public void logonToDevice(PaymentDevice paymentDevice) throws DataException, SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = connectPort(paymentDevice.getAddress());
		// If there is an error while opening the port, there is no need to close it
		try {
			handleResponseExtended(logonToDevice(port));

			// TODO

			return;

		} catch(Exception e) {
			throw e;

		} finally {
			port.closePort();
		}

	}

	@Override
	public void getLastApprovedTransaction(PaymentDevice paymentDevice) throws DataException, SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = connectPort(paymentDevice.getAddress());

		try {
			byte[] message = getLastApprovedTransaction(port);
			handleResponseExtended(message);

			// TODO

			return;

		} catch(Exception e) {
			throw e;

		} finally {
			port.closePort();
		}

	}

	@Override
	public byte[] callTMS(PaymentDevice paymentDevice) throws DataException, SerialPortInvalidPortException,
	PortInUseException, IOException {

		SerialPort port = connectPort(paymentDevice.getAddress());

		try {
			byte[] message =  sendMessageOnlyHeader(FunctionCode.TMS, port, 23);
			return message;

			// TODO
		} catch (Exception e) {
			throw e;
		} finally {
			port.closePort();
		}
	}

	@Override
	public void performSettlements(PaymentDevice paymentDevice) throws DataException, SerialPortInvalidPortException,
	PortInUseException, IOException {
		SerialPort port = connectPort(paymentDevice.getAddress());

		try {
			byte[] message = performSettlement(port);
			handleResponseExtended(message);

			// TODO

			return;

		} catch(Exception e) {
			throw e;

		} finally {
			port.closePort();
		}
	}

	private byte[] sendMessageOnlyHeader(String functionCode, SerialPort port, int length) throws IOException {
		byte[] message = createMessage(createMessageHeader(functionCode, netsVersionCode));
		byte[] response = new byte[length];

		sendAndReceiveMessage(port, message, response);

		return response;
	}

	private void sendAndReceiveMessage(SerialPort port, byte[] message, byte[] response) throws IOException {
		port.flushIOBuffers();
		Rs232Util.sendWithAckCheck(port, message, totalTries);
		receiveMessage(port, response);
	}

	private byte[] requestDeviceStatus(SerialPort port) throws IOException {
		return sendMessageOnlyHeader(FunctionCode.REQUEST_TERMINAL_STATUS, port, 23);
	}

	private byte[] logonToDevice(SerialPort port) throws IOException {
		return sendMessageOnlyHeader(FunctionCode.LOGON, port, 215);
	}

	private byte[] getLastApprovedTransaction(SerialPort port) throws IOException {
		byte[] response = sendMessageOnlyHeader(FunctionCode.GET_LAST_APPROVED_TRANSACTION, port, 512);

		response = Rs232Util.truncateMessage(response);

		return response;
	}

	private byte[] performSettlement(SerialPort port) throws IOException {
		byte[] response = sendMessageOnlyHeader(FunctionCode.SETTLEMENT, port, 1024);

		response = Rs232Util.truncateMessage(response);

		return response;
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
	public String handleResponseExtended(byte[] response) throws DataException {
		String respCode = bytesToUtf8(Arrays.copyOfRange(response, 17, 19));

		if(handleResponseCode(respCode)) {
			return ResponseCode.APPROVED;
		}

		// Come here if no error was thrown in handleResponseCode, or if the default case was hit

		switch(respCode) {
		case ResponseCode.AUTHORISATATION_REQUEST_DECLINED:
			// TODO
			throw new DataException("Authorisation request declined", "Please refer to field code HC for more information");

		case ResponseCode.UNABLE_TO_READ_CARD:
			// TODO
			throw new DataException("Unable to read card", "Make sure the card was inserted/swiped/tapped correctly");

		case ResponseCode.INVALID_CARD:
			// TODO
			throw new DataException("Invalid card", "Make sure the card used was valid");

		case ResponseCode.LOGON_REQUIRED:
			// TODO
			throw new DataException("Unable to log on", "The application was not able to log on the terminal.");
			//			return;

		case ResponseCode.CALLBACK_DISPLAY_MESSAGE:
			// TODO
			throw new DataException("Authorisation request timeout w/ Callback", "The authorisation request did not receive a response. Please contact NETS.");

		case ResponseCode.CALLBACK_CARD_APP_SELECTION:
			// TODO
			throw new DataException("Card app selection callback", "Prompt user to select which credit card scheme app should be used for the application.");

		default:
			throw new DataException("Unknown response", "Unknown response while requesting terminal status: `" + respCode + "`. Please contact admin.");
		}
	}

	private SerialPort connectPort(String portName) throws PortInUseException, SerialPortInvalidPortException {
		return Rs232Util.connect(portName, 9600, 8, 1, SerialPort.NO_PARITY, 1800, true, false);
	}

	private void receiveMessage(SerialPort port, byte[] response) throws IOException {
		Rs232Util.readWithAckCheck(port, response, totalTries, checkLrc);


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
			}
		}
	}

}
