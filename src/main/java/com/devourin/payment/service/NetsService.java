package com.devourin.payment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.devourin.payment.exception.DataException;
import com.devourin.payment.exception.PortInUseException;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.model.PaymentInfo;
import com.devourin.payment.util.RandomUtil;
import com.devourin.payment.util.Rs232Util;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@Service
public interface NetsService {

	class ResponseCode { // Reduce magic numbers
		final static String APPROVED = "00";
		final static String FUNCTION_NOT_AVAILABLE = "01"; // Feature not available for POS to use
		final static String TERMINAL_IN_PRE_ACTIVATION = "02"; // Contact NETS. The terminal is not ready to receive any function request.
		final static String FAULTY_IUC = "03"; // Check that the contactless reader is connected
		final static String FAULTY_IUR = "04"; // Check that the contact reader is connected
		final static String THIRD_PARTY_APP_NOT_AVAILABLE = "05"; // Check with terminal provider on application loading.
		final static String MANDATORY_FIELD_CODE_MISSING = "20"; // Check the message being sent to the terminal.
		final static String AUTHORISATATION_REQUEST_DECLINED = "51"; // Refer to the field code HC for more information
		final static String UNABLE_TO_READ_CARD = "C0"; // Check the card was inserted/swiped/tapped correctly
		final static String INVALID_CARD = "C1"; // Make sure the card used was valid
		final static String TERMINAL_CONNECTION_PROBLEM = "HS"; // Check terminal connectivity (SIM Card, Ethernet etc.)
		final static String LOGON_REQUIRED = "LR"; // Please logon to the terminal
		final static String AUTHORISATION_REQUEST_TIMEOUT = "TO"; // Contact NETS. Terminal did not receive a response while sending an authorisation request.
		final static String CALLBACK_DISPLAY_MESSAGE = "Z0"; // Contact NETS. Terminal did not receive a response while sending an authorisation request.
		final static String CALLBACK_CARD_APP_SELECTION = "Z1"; // Prompt user to select which credit card scheme app should be used for the application
		final static String SETTLEMENT_REQUIRED = "SF"; // Terminal requires settlement for all acquirers.
	}

	class FunctionCode { // Reduce magic numbers
		final static String REQUEST_TERMINAL_STATUS = "55";
		final static String GET_LAST_APPROVED_TRANSACTION = "56";
		final static String LOGON = "80";
		final static String SETTLEMENT = "81";
		final static String TMS = "84";

	}

	class FieldCodes { // Annex B
		final static String APPROVEl_CODE = "01";
		final static String RESPONSE_TEXT = "02";
		final static String TRANSACTION_DATE = "03";
		final static String TRANSACTION_TIME = "04";
		final static String TERMINAL_ID = "16";
		final static String AUTHORIZATION_ID = "17";
		final static String CARD_NUMBER = "30";
		final static String TRANSACTION_AMOUNT = "40";
		final static String SERVICE_FEE = "41";
		final static String CASH_BACK_AMOUNT = "42";
		final static String LOYALTY_REDEMPTION = "43";
		final static String STAN = "65";
		final static String ACQUIRER_NAME = "9G";
		final static String INVOICE_NUMBER = "9H";
		final static String TRANSACTION_STATUS = "CP";
		final static String MERCHANT_ID = "D1";
		final static String TRANSACTION_TYPE_INDICATOR = "T2";
		final static String ENHANCED_ECR_REFERENCE_NUMBER = "HD";
		final static String MERCHANT_NAME_AND_ADDRESS = "D0";
		final static String RETRIEVAL_REFERENCE_NUMBER = "D3";
		final static String CARD_NAME = "L7";
		final static String POS_MESSAGE = "L5";
		final static String RESPONSE_MESSAGE_I = "R0";
		final static String RESPONSE_MESSAGE_II = "R1";
		final static String LOYALTY_PROGRAM_NAME = "L1";
		final static String LOYALTY_TYPE = "L2";
		final static String REDEMPTION_VALUE = "L3";
		final static String CURRENT_BALANCE = "L4";
		final static String LOYALTY_MARKETING_MESSAGE = "L6";
		final static String LOYALTY_PROGRAM_EXP_DATE = "L8";
		final static String LOYALTY_MARKETING_MESSAGE1 = "L9";
		final static String HOST_RESPONSE_CODE = "HC";
		final static String CARD_ENTRY_MODE = "CN";
		final static String RECEIPT_TEXT_FORMAT = "RP";
		final static String MOBILE_NUMBER = "MN";
		final static String LAST_AMOUNT = "F7";
		final static String CALLBACK_FUNCTION_TYPE = "M0";
		final static String MESSAGE_PAYLOAD = "M7";
		final static String MESSAGE_INDEX = "M1";
		final static String MESSAGE = "M2";
		final static String NUMBER_OF_SELECTION = "M3";
		final static String SELECTION_INDEX = "M4";
		final static String SELECTION_LABEL = "M5";
		final static String	SELECTION_NAME = "M6";
		final static String SCHEME_CATEGORY = "9Q";
		final static String CARD_TYPE = "9M";
		final static String CARD_ISSUER_NAME = "D2";
		final static String POS_ID = "9I";
		final static String TRANSACTION_ID = "A1";
		final static String CARD_BALANCE = "HE";
		final static String FOREIGN_AMOUNT = "FA";
		final static String FOREIGN_MID = "F0";
		final static String CURRENCY_NAME = "F2";
		final static String CARD_HOLDER_NAME = "D6";
		final static String RFU4 = "31"; // ALSO CARD EXPIRY DATE
		final static String AID ="9A";
		final static String TRANSACTION_CERTIFICATE = "9D";
		final static String APPLICATION_PROFILE = "9B";
		final static String CID = "9C";
		final static String TSI = "9F";
		final static String TVR = "9E";
		final static String BATCH_NUMBER = "50";
		final static String PROCESSING_GATEWAY ="D7";
		final static String OFFLINE_TXN_TYPE = "O1";

	}

	static final int totalTries = 3;
	static final String netsVersionCode = "01";

	void createPayment(PaymentInfo body, PaymentDevice paymentDevice)
			throws DataException, SerialPortInvalidPortException, PortInUseException, IOException;

	void logonToDevice(PaymentDevice paymentDevice)
			throws DataException, SerialPortInvalidPortException, PortInUseException, IOException;

	void getLastApprovedTransaction(PaymentDevice paymentDevice)
			throws DataException, SerialPortInvalidPortException, PortInUseException, IOException;

	void performSettlements(PaymentDevice paymentDevice)
			throws DataException, SerialPortInvalidPortException, PortInUseException, IOException;

	byte[] callTMS(PaymentDevice paymentDevice)
			throws DataException, SerialPortInvalidPortException, PortInUseException, IOException;

	byte[] createMessage(byte[] body);

	String handleResponseExtended(byte[] response) throws DataException;

	default byte[] createMessageHeader(String functionCode, String versionCode) {
		ByteArrayOutputStream header = new ByteArrayOutputStream(18);

		header.write(generateEcn(), 0, 12); // ECN is always 12 bytes
		header.write(getUtf8Bytes(functionCode), 0, 2); // Function Code is always 2 bytes
		header.write(getUtf8Bytes(versionCode), 0, 2); // Version Code is always 2 bytes
		addRFU(header);
		addSeparator(header);

		return header.toByteArray();
	}

	default byte[] createMessageData(String fieldCode, String data) {
		ByteArrayOutputStream messageData = new ByteArrayOutputStream(data.length() + 5);

		messageData.write(getUtf8Bytes(fieldCode), 0, 2);
		writeLengthInBcd(messageData, data.length());
		messageData.write(getUtf8Bytes(data), 0, data.length());
		addSeparator(messageData);

		return messageData.toByteArray();
	}

	default int getBcd(int number) {
		int bcd = 0x00;
		int shiftCounter = 0;

		while (number > 0) {
			bcd |= (number % 10) << (shiftCounter++ << 2);
			number /= 10;
		}

		return bcd;
	}

	default void writeLengthInBcd(ByteArrayOutputStream stream, int length) {
		int bcd = getBcd(length);

		stream.write((byte) (bcd >>> 8)); // thousands and hundreds
		stream.write((byte) bcd); // tens and ones
	}

	default byte[] generateEcn() {
		return getUtf8Bytes(RandomUtil.generateRandomIdWithCurrentTime(2));
	}

	default void addRFU(ByteArrayOutputStream stream) {
		stream.write(0x30);
	}

	default void addSeparator(ByteArrayOutputStream stream) {
		stream.write(0x1C);
	}

	default byte generateLrc(byte[] arr) {
		byte lrc = 0x00;

		for(int i = 0; i < arr.length; i++) {
			lrc ^= arr[i];
			if(arr[i] == Rs232Util.ETX) break; // End of first message, if a repeat was sent
		}

		return lrc;
	}

	default byte[] addLrc(ByteArrayOutputStream message, int skipFromStart) {
		message.write(0x00); // Dummy LRC value to keep array size consistent
		byte[] messageArray = message.toByteArray();

		byte[] lrcArray = Arrays.copyOfRange(
				messageArray, skipFromStart, messageArray.length - 1); // Skip last value since it is for LRC
		messageArray[messageArray.length - 1] = generateLrc(lrcArray); // Set actual LRC value for message

		return messageArray;
	}

	default boolean checkLrc(byte[] message, int skipFromStart) {
		if(skipFromStart >= message.length) {
			return false;
		}
		byte[] lrcArray = Arrays.copyOfRange(message, skipFromStart, message.length - 1);
		return message[message.length - 1] == generateLrc(lrcArray);
	}

	default byte[] concatByteArrays(byte[]... bytes) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		for(byte[] arr : bytes) {
			stream.write(arr, 0, arr.length);
		}

		return stream.toByteArray();
	}

	default byte[] getUtf8Bytes(String str) {
		return str.getBytes(StandardCharsets.UTF_8);
	}
	default String bytesToUtf8(byte[] arr) {
		return new String(arr, StandardCharsets.UTF_8);
	}

	/**
	 * 
	 * @param responseCode
	 * @return <code>true</code> if the request was approved, <code>false</code> if none of the cases were hit.
	 * @throws DataException An error describing what the response was, if the request was not approved and one of the cases was hit. This usually describes an error in the setup of the terminal, which needs to be fixed before proper usage. It could also describe an error made by faulty code.
	 */
	default boolean handleResponseCode(String responseCode) throws DataException {
		switch(responseCode) {
		case ResponseCode.APPROVED:
			return true;

		case ResponseCode.FUNCTION_NOT_AVAILABLE:
			throw new DataException("Function not available", "The function specified does not exist.");

		case ResponseCode.TERMINAL_IN_PRE_ACTIVATION:
			throw new DataException("Terminal in pre-activation", "The terminal is not ready to receive any function request. Contact NETS.");

		case ResponseCode.FAULTY_IUC:
			throw new DataException("Faulty IUC", "Check that the contactless reader is connected.");

		case ResponseCode.FAULTY_IUR:
			throw new DataException("Faulty IUR", "Check that the contact reader is connected.");

		case ResponseCode.THIRD_PARTY_APP_NOT_AVAILABLE:
			throw new DataException("Third-party app not available", "Check with the terminal provider on application loading.");

		case ResponseCode.MANDATORY_FIELD_CODE_MISSING:
			throw new DataException("Mandatory field code missing", "Check the message being sent to the terminal");

		case ResponseCode.TERMINAL_CONNECTION_PROBLEM:
			throw new DataException("Terminal connection problem", "Make sure the terminal is connected to the internet (Ethernet, SIM Card etc.)");

		case ResponseCode.AUTHORISATION_REQUEST_TIMEOUT:
			throw new DataException("Authorisation request timeout", "The authorisation request did not receive a response. Please contact NETS.");

		case ResponseCode.SETTLEMENT_REQUIRED:
			throw new DataException("Settlement required", "Terminal requires settlement for all acquirers.");

		default:
			return false;
		}
	}
}

