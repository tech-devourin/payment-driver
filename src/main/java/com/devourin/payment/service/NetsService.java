package com.devourin.payment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devourin.payment.constant.Model;
import com.devourin.payment.constant.PaymentMethod;
import com.devourin.payment.exception.DataException;
import com.devourin.payment.exception.PortInUseException;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.model.PaymentInfo;
import com.devourin.payment.util.RandomUtil;
import com.devourin.payment.util.Rs232Util;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@Service
public interface NetsService {

	static Logger logger = LoggerFactory.getLogger(NetsService.class);

	static byte[] emptyArray = {};

	class ResponseCode { // Reduce magic numbers
		private ResponseCode() {}

		static final String APPROVED = "00";
		static final String FUNCTION_NOT_AVAILABLE = "01"; // Feature not available for POS to use
		static final String TERMINAL_IN_PRE_ACTIVATION = "02"; // Contact NETS. The terminal is not ready to receive any function request.
		static final String FAULTY_IUC = "03"; // Check that the contactless reader is connected
		static final String FAULTY_IUR = "04"; // Check that the contact reader is connected
		static final String THIRD_PARTY_APP_NOT_AVAILABLE = "05"; // Check with terminal provider on application loading.
		static final String MANDATORY_FIELD_CODE_MISSING = "20"; // Check the message being sent to the terminal.
		static final String AUTHORISATATION_REQUEST_DECLINED = "51"; // Refer to the field code HC for more information
		static final String UNABLE_TO_READ_CARD = "C0"; // Check the card was inserted/swiped/tapped correctly
		static final String INVALID_CARD = "C1"; // Make sure the card used was valid
		static final String TERMINAL_CONNECTION_PROBLEM = "HS"; // Check terminal connectivity (SIM Card, Ethernet etc.)
		static final String LOGON_REQUIRED = "LR"; // Please logon to the terminal
		static final String AUTHORISATION_REQUEST_TIMEOUT = "TO"; // Contact NETS. Terminal did not receive a response while sending an authorisation request.
		static final String CALLBACK_DISPLAY_MESSAGE = "Z0"; // Contact NETS. Terminal did not receive a response while sending an authorisation request.
		static final String CALLBACK_CARD_APP_SELECTION = "Z1"; // Prompt user to select which credit card scheme app should be used for the application
		static final String SETTLEMENT_REQUIRED = "SF"; // Terminal requires settlement for all acquirers.
		static final String CANCELLED_BY_USER = "US"; // Transaction cancelled by the user
	}

	class FunctionCode { // Reduce magic numbers
		private FunctionCode() {}

		static final String REQUEST_TERMINAL_STATUS = "55";
		static final String GET_LAST_APPROVED_TRANSACTION = "56";
		static final String PURCHASE = "00";
		static final String LOGON = "80";
		static final String SETTLEMENT = "81";
		static final String TMS = "84";
		static final String NETS_PURCHASE = "30";
		static final String CREDIT_CARD_PURCHASE = "I0";
		static final String UPI_PURCHASE = "31";
		static final String BCA_PURCHASE = "65";
		static final String RUPAY_PURCHASE = "6A";
		static final String MYDEBIT_PURCHASE = "6B";
	}

	class FieldCode { // Annex B
		private FieldCode() {}

		static final String APPROVAL_CODE = "01";
		static final String RESPONSE_TEXT = "02";
		static final String TRANSACTION_DATE = "03";
		static final String TRANSACTION_TIME = "04";
		static final String TERMINAL_ID = "16";
		static final String AUTHORIZATION_ID = "17";
		static final String CARD_NUMBER = "30";
		static final String TRANSACTION_AMOUNT = "40";
		static final String SERVICE_FEE = "41";
		static final String CASH_BACK_AMOUNT = "42";
		static final String LOYALTY_REDEMPTION = "43";
		static final String STAN = "65";
		static final String ACQUIRER_NAME = "9G";
		static final String INVOICE_NUMBER = "9H";
		static final String TRANSACTION_STATUS = "CP";
		static final String MERCHANT_ID = "D1";
		static final String TRANSACTION_TYPE_INDICATOR = "T2";
		static final String ENHANCED_ECR_REFERENCE_NUMBER = "HD";
		static final String MERCHANT_NAME_AND_ADDRESS = "D0";
		static final String RETRIEVAL_REFERENCE_NUMBER = "D3";
		static final String CARD_NAME = "L7";
		static final String POS_MESSAGE = "L5";
		static final String RESPONSE_MESSAGE_I = "R0";
		static final String RESPONSE_MESSAGE_II = "R1";
		static final String LOYALTY_PROGRAM_NAME = "L1";
		static final String LOYALTY_TYPE = "L2";
		static final String REDEMPTION_VALUE = "L3";
		static final String CURRENT_BALANCE = "L4";
		static final String LOYALTY_MARKETING_MESSAGE = "L6";
		static final String LOYALTY_PROGRAM_EXP_DATE = "L8";
		static final String LOYALTY_MARKETING_MESSAGE1 = "L9";
		static final String HOST_RESPONSE_CODE = "HC";
		static final String CARD_ENTRY_MODE = "CN";
		static final String RECEIPT_TEXT_FORMAT = "RP";
		static final String MOBILE_NUMBER = "MN";
		static final String LAST_AMOUNT = "F7";
		static final String CALLBACK_FUNCTION_TYPE = "M0";
		static final String MESSAGE_PAYLOAD = "M7";
		static final String MESSAGE_INDEX = "M1";
		static final String MESSAGE = "M2";
		static final String NUMBER_OF_SELECTION = "M3";
		static final String SELECTION_INDEX = "M4";
		static final String SELECTION_LABEL = "M5";
		static final String	SELECTION_NAME = "M6";
		static final String SCHEME_CATEGORY = "9Q";
		static final String CARD_TYPE = "9M";
		static final String CARD_ISSUER_NAME = "D2";
		static final String POS_ID = "9I";
		static final String TRANSACTION_ID = "A1";
		static final String CARD_BALANCE = "HE";
		static final String FOREIGN_AMOUNT = "FA";
		static final String FOREIGN_MID = "F0";
		static final String CURRENCY_NAME = "F2";
		static final String CARD_HOLDER_NAME = "D6";
		static final String RFU4 = "31"; // ALSO CARD EXPIRY DATE
		static final String AID ="9A";
		static final String TRANSACTION_CERTIFICATE = "9D";
		static final String APPLICATION_PROFILE = "9B";
		static final String CID = "9C";
		static final String TSI = "9F";
		static final String TVR = "9E";
		static final String BATCH_NUMBER = "50";
		static final String PROCESSING_GATEWAY ="D7";
		static final String OFFLINE_TXN_TYPE = "O1";

	}

	class VersionCode {
		private VersionCode() {}

		public static final String NETS = "01";
		public static final String UOB = "02";
	}

	class Codes {
		private String function;
		private String version;

		public String getFunction() {
			return function;
		}
		public void setFunction(String function) {
			this.function = function;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
	}

	static final int TOTAL_TRIES = 3;

	void createPayment(PaymentInfo body, PaymentDevice paymentDevice, ListeningService listeningService)
			throws SerialPortInvalidPortException, PortInUseException, IOException;

	void requestDeviceStatus(PaymentDevice paymentDevice, ListeningService listeningService)
			throws SerialPortInvalidPortException, PortInUseException, IOException;

	void logonToDevice(PaymentDevice paymentDevice, ListeningService listeningService)
			throws SerialPortInvalidPortException, PortInUseException, IOException;

	void getLastApprovedTransaction(PaymentDevice paymentDevice, ListeningService listeningService)
			throws SerialPortInvalidPortException, PortInUseException, IOException;

	void performSettlements(PaymentDevice paymentDevice, ListeningService listeningService)
			throws SerialPortInvalidPortException, PortInUseException, IOException;

	void callTMS(PaymentDevice paymentDevice, ListeningService listeningService)
			throws SerialPortInvalidPortException, PortInUseException, IOException;

	byte[] createMessage(byte[] body);

	PaymentMessagingService getPaymentMessagingService();

	default byte[] createMessageHeader(String functionCode, String versionCode, Map<Model, Map<String, Long>> paymentIdMap) {
		ByteArrayOutputStream header = new ByteArrayOutputStream(18);

		header.write(generateEcn(paymentIdMap), 0, 12); // ECN is always 12 bytes
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

	default int getDecimal(int number) {
		int dec = 0;
		int counter = 0;

		while(number > 0) {
			dec += (number & 0x0F) * java.lang.Math.pow(10, counter++);
			number >>= 4;
		}

		return dec;
	}

	default void writeLengthInBcd(ByteArrayOutputStream stream, int length) {
		int bcd = getBcd(length);

		stream.write((byte) (bcd >>> 8)); // thousands and hundreds
		stream.write((byte) bcd); // tens and ones
	}

	default byte[] generateEcn(Map<Model, Map<String, Long>> paymentIdMap) {
		String ecn = RandomUtil.generateRandomIdWithCurrentTime(2);
		Map<String, Long> map = paymentIdMap.get(Model.NETS);
		while(map.containsKey(ecn)) {
			ecn = RandomUtil.generateRandomIdWithCurrentTime(2);
		}
		map.put(ecn, Instant.now().getEpochSecond());
		return getUtf8Bytes(ecn);
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

	default byte[] getOnlyBodyFromMessage(byte[] message) {
		int i;
		for(i = 0; i < message.length; i++) {
			if(message[i] == 0x1C) {
				i++;
				break;
			}
		}

		return Arrays.copyOf(message, i);
	}

	default void handleAsyncMessageHandling(ListeningService listeningService, byte[] message, Map<Model, Map<String, Long>> paymentIdMap, PaymentDevice paymentDevice) throws DataException, SerialPortInvalidPortException, PortInUseException, IOException {
		message = generateHomogenisedMessage(message);

		String ecn = bytesToUtf8(Arrays.copyOfRange(message, 0, 12));

		String functionCode = bytesToUtf8(Arrays.copyOfRange(message, 12, 14));

		Map<String, Long> ecnMap = paymentIdMap.get(Model.NETS);

		if(!ecnMap.containsKey(ecn)) {
			return;
		}

		ecnMap.remove(ecn);

		handleResponse(message, paymentDevice, listeningService);

		switch(functionCode) {
		case FunctionCode.SETTLEMENT:
		case FunctionCode.TMS:
			logonToDevice(paymentDevice, listeningService);
			return;

		case FunctionCode.LOGON:
			requestDeviceStatus(paymentDevice, listeningService);
			return;

		case FunctionCode.NETS_PURCHASE:
		case FunctionCode.CREDIT_CARD_PURCHASE:
		case FunctionCode.UPI_PURCHASE:
		case FunctionCode.BCA_PURCHASE:
		case FunctionCode.RUPAY_PURCHASE:
		case FunctionCode.MYDEBIT_PURCHASE:
			getPaymentMessagingService().sendWebsocketSuccess();
			return;

		default:
		}
	}

	default Map<String, byte[]> getMapFromBody(byte[] body) {

		if(logger.isInfoEnabled()) {
			logger.info(String.format("Received: %s", Rs232Util.getHexString(body)));
		}
		Map<String, byte[]> map = new HashMap<>();
		int messageLength = body.length;
		int i = 0;
		while(i < messageLength) {
			String fieldCode = bytesToUtf8(Arrays.copyOfRange(body, i, i + 2));
			int len = getDecimal((body[i + 2]) << 8) | body[i + 3];

			i += 4;

			map.put(fieldCode, Arrays.copyOfRange(body, i, i + len));

			i += len + 1;
		}

		return map;
	}

	default Map<String, byte[]> getBodyMapFromMessage(byte[] message) {
		return getMapFromBody(getOnlyBodyFromMessage(message));
	}

	byte[] generateHomogenisedMessage(byte[] message);

	default void handleTransactionResponse(Map<String, byte[]> bodyMap) {

	}

	/**
	 * 
	 * @param response The homogenised response that needs to be handled
	 * @return <b>String</b>
	 * @throws DataException An error describing what the response was, if the request was not approved and one of the cases was hit. This usually describes an error in the setup of the terminal, which needs to be fixed before proper usage. It could also describe an error made by faulty code.
	 * @throws IOException 
	 * @throws PortInUseException 
	 * @throws SerialPortInvalidPortException 
	 */
	default String handleResponse(byte[] response, PaymentDevice paymentDevice, ListeningService listeningService) throws DataException, SerialPortInvalidPortException, PortInUseException, IOException {
		String respCode = bytesToUtf8(Arrays.copyOfRange(response, 14, 16));

		switch(respCode) {
		case ResponseCode.APPROVED:
			return ResponseCode.APPROVED;

		case ResponseCode.SETTLEMENT_REQUIRED:
			performSettlements(paymentDevice, listeningService);
			throw new DataException("Transaction declined", "The transaction was declined. Please try again.");

		case ResponseCode.CANCELLED_BY_USER:
		case ResponseCode.UNABLE_TO_READ_CARD:
		case ResponseCode.INVALID_CARD:
		case ResponseCode.AUTHORISATATION_REQUEST_DECLINED:
			throw new DataException("Transaction declined", "The transaction was declined. Please try again.");

		case ResponseCode.TERMINAL_IN_PRE_ACTIVATION:
		case ResponseCode.FAULTY_IUC:
		case ResponseCode.FAULTY_IUR:
		case ResponseCode.AUTHORISATION_REQUEST_TIMEOUT:
		case ResponseCode.CALLBACK_DISPLAY_MESSAGE:
			throw new DataException("Problem with terminal", "There is an issue with the terminal. Please contact NETS.");

		case ResponseCode.TERMINAL_CONNECTION_PROBLEM:
			throw new DataException("Terminal connection problem", "Make sure the terminal is connected to the internet (Ethernet, SIM Card etc.)");

		case ResponseCode.LOGON_REQUIRED:
			throw new DataException("Unable to log on", "Please restart the computer, or contact NETS.");

		case ResponseCode.THIRD_PARTY_APP_NOT_AVAILABLE:
			throw new DataException("Third-party app not available", "Check with the terminal provider on application loading.");

		case ResponseCode.MANDATORY_FIELD_CODE_MISSING:
			throw new DataException("Mandatory field code missing", "Check the message being sent to the terminal");

		case ResponseCode.FUNCTION_NOT_AVAILABLE:
			throw new DataException("Function not available", "The function specified does not exist.");

		default:
			throw new DataException("Unknown response", "Unknown response while requesting terminal status: `" + respCode + "`. Please contact admin.");
		}
	}

	default Codes getPaymentCodes(PaymentMethod method) {
		Codes codes = new Codes();
		codes.version = VersionCode.NETS;

		switch(method) {
		case NETS:
			codes.function = FunctionCode.NETS_PURCHASE;
			return codes;
		case CREDIT_CARD:
			codes.function = FunctionCode.CREDIT_CARD_PURCHASE;
			return codes;

		case UNIONPAY:
			codes.function = FunctionCode.UPI_PURCHASE;
			return codes;

		case BCA:
			codes.function = FunctionCode.BCA_PURCHASE;
			return codes;

		case RUPAY:
			codes.function = FunctionCode.RUPAY_PURCHASE;
			return codes;

		case MYDEBIT:
			codes.function = FunctionCode.MYDEBIT_PURCHASE;
			return codes;

		case DEBIT_CARD:
			codes.version = VersionCode.UOB;
			return codes;
		default:
			return null;
		}
	}
}

