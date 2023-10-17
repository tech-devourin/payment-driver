package com.devourin.payment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import com.devourin.payment.exception.DeviceException;
import com.devourin.payment.exception.NeedToRestartPaymentException;
import com.devourin.payment.exception.PortInUseException;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.model.PaymentInfo;
import com.devourin.payment.model.ReturnedPaymentInfo;
import com.devourin.payment.util.RandomUtil;
import com.devourin.payment.util.Rs232Util;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@Service
public interface NetsService {

	static Logger logger = LoggerFactory.getLogger(NetsService.class);

	static byte[] emptyArray = {};

	SimpleDateFormat yyMMdd = new SimpleDateFormat("yyMMdd");
	SimpleDateFormat ddMMMyyyy = new SimpleDateFormat("dd MMM yyyy");

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
		static final String INVALID_CARD1 = "C1"; // Make sure the card used was valid
		static final String INVALID_CARD2 = "14"; // Make sure the card used was valid
		static final String INVALID_CARD3 = "78"; // Make sure the card used was valid
		static final String INVALID_CARD4 = "81"; // Make sure the card used was valid
		static final String INVALID_CARD5 = "85"; // Make sure the card used was valid
		static final String INVALID_CARD6 = "D6"; // Make sure the card used was valid
		static final String INVALID_CARD7 = "IC"; // Make sure the card used was valid
		static final String TERMINAL_CONNECTION_PROBLEM = "HS"; // Check terminal connectivity (SIM Card, Ethernet etc.)
		static final String LOGON_REQUIRED = "LR"; // Please logon to the terminal
		static final String TIMEOUT = "TO"; // Contact NETS. Terminal did not receive a response while sending an authorisation request.
		static final String CALLBACK_DISPLAY_MESSAGE = "Z0"; // Contact NETS. Terminal did not receive a response while sending an authorisation request.
		static final String CALLBACK_CARD_APP_SELECTION = "Z1"; // Prompt user to select which credit card scheme app should be used for the application
		static final String SETTLEMENT_REQUIRED = "SF"; // Terminal requires settlement for all acquirers.
		static final String CANCELLED_BY_USER = "US"; // Transaction cancelled by the user
		static final String OUT_OF_PAPER = "GX"; // The terminal is out of paper
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
		static final String TRANSACTION_AMOUNT = "40";
		static final String STAN = "65";
		static final String TRANSACTION_TYPE_INDICATOR = "T2";
		static final String FUNCTION_CODE = "FC";
		static final String CARD_TYPE = "9M";
	}

	class VersionCode {
		private VersionCode() {}

		public static final String NETS = "01";
		public static final String UOB = "02";
	}

	class Codes {
		private String function;
		private String version;

		private Codes(String version, String function) {
			this.function = function;
			this.version = version;
		}

		public static Codes nets(String function) {
			return new Codes(VersionCode.NETS, function);
		}

		public static Codes uob(String function) {
			return new Codes(VersionCode.UOB, function);
		}

		public String getFunction() {
			return function;
		}
		public String getVersion() {
			return version;
		}
	}

	static final int TOTAL_TRIES = 3;

	void createPayment(PaymentInfo body, PaymentDevice paymentDevice, ListeningService listeningService)
			throws SerialPortInvalidPortException, PortInUseException, IOException, DataException;

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

		return Arrays.copyOfRange(message, i, message.length - 1);
	}

	default void handleAsyncMessageHandling(ListeningService listeningService, byte[] message, Map<Model, Map<String, Long>> paymentIdMap, PaymentDevice paymentDevice) throws SerialPortInvalidPortException, PortInUseException, IOException, NeedToRestartPaymentException, DeviceException {
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
			callTMS(paymentDevice, listeningService);
			return;

		case FunctionCode.TMS:
			logonToDevice(paymentDevice, listeningService);
			return;

		case FunctionCode.LOGON:
			requestDeviceStatus(paymentDevice, listeningService);
			return;

		case FunctionCode.GET_LAST_APPROVED_TRANSACTION:
			getPaymentMessagingService().sendWebsocketSuccess(getPaymentDetails(message, true, functionCode));
			return;

		case FunctionCode.NETS_PURCHASE:
		case FunctionCode.CREDIT_CARD_PURCHASE:
		case FunctionCode.UPI_PURCHASE:
		case FunctionCode.BCA_PURCHASE:
		case FunctionCode.RUPAY_PURCHASE:
		case FunctionCode.MYDEBIT_PURCHASE:
			getPaymentMessagingService().sendWebsocketSuccess(getPaymentDetails(message, false, functionCode));
			return;

		default:
		}
	}

	default ReturnedPaymentInfo getPaymentDetails(byte[] message, boolean isPreviousPayment, String functionCode) {
		Map<String, byte[]> bodyMap = getBodyMapFromMessage(message);
		ReturnedPaymentInfo paymentSuccess = new ReturnedPaymentInfo();

		paymentSuccess.setPreviousPayment(isPreviousPayment);

		if(bodyMap.containsKey(FieldCode.RESPONSE_TEXT)) {
			paymentSuccess.setResponseText(getStringFromByteArray(bodyMap.get(FieldCode.RESPONSE_TEXT)).strip());
		}

		if(bodyMap.containsKey(FieldCode.TERMINAL_ID)) {
			paymentSuccess.setDeviceId(getStringFromByteArray(bodyMap.get(FieldCode.TERMINAL_ID)));
		}

		if(bodyMap.containsKey(FieldCode.TRANSACTION_AMOUNT)) {
			paymentSuccess.setTransactionAmount(Integer.parseInt(getStringFromByteArray(bodyMap.get(FieldCode.TRANSACTION_AMOUNT))));
		}

		if(bodyMap.containsKey(FieldCode.STAN)) {
			paymentSuccess.setStan(getStringFromByteArray(bodyMap.get(FieldCode.STAN)));
		}

		if(bodyMap.containsKey(FieldCode.APPROVAL_CODE)) {
			paymentSuccess.setApprovalCode(getStringFromByteArray(bodyMap.get(FieldCode.APPROVAL_CODE)));
		}

		if(!isPreviousPayment) {
			paymentSuccess.setPaymentMethod(getPaymentMethodFromFunctionCode(functionCode));
		} else if(bodyMap.containsKey(FieldCode.FUNCTION_CODE)) {
			String funcCode = getStringFromByteArray(bodyMap.get(FieldCode.FUNCTION_CODE));
			paymentSuccess.setPaymentMethod(getPaymentMethodFromFunctionCode(funcCode));
		}

		if(bodyMap.containsKey(FieldCode.CARD_TYPE)) {
			paymentSuccess.setCardType(getStringFromByteArray(bodyMap.get(FieldCode.CARD_TYPE)));
		}

		if(bodyMap.containsKey(FieldCode.TRANSACTION_DATE)) {
			try {
				// Convert date from `yyMMdd` to `dd MMM yyyy`
				paymentSuccess.setDate(ddMMMyyyy.format(yyMMdd.parse(getStringFromByteArray(bodyMap.get(FieldCode.TRANSACTION_DATE)))));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if(bodyMap.containsKey(FieldCode.TRANSACTION_TIME)) {
			String time = getStringFromByteArray(bodyMap.get(FieldCode.TRANSACTION_TIME));
			// Convert time from `hhmmss` to `hh:mm:ss`
			paymentSuccess.setTime(String.format("%s:%s:%s", time.substring(0, 2), time.substring(2, 4), time.substring(4, 6)));

		}

		return paymentSuccess;
	}

	default Map<String, byte[]> getMapFromBody(byte[] body) {
		Map<String, byte[]> map = new HashMap<>();
		int messageLength = body.length;
		int i = 0;
		while(i < messageLength) {
			String fieldCode = bytesToUtf8(Arrays.copyOfRange(body, i, i + 2));

			i += 4;

			ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
			while(i < messageLength && body[i] != 0x1c) {
				if(body[i] != 0x00) {
					byteArrayOS.write(body[i]);
				}
				i++;
			}
			map.put(fieldCode, byteArrayOS.toByteArray());

			i++;
		}

		return map;
	}

	default Map<String, byte[]> getBodyMapFromMessage(byte[] message) {
		return getMapFromBody(getOnlyBodyFromMessage(message));
	}

	default String getStringFromByteArray(byte[] arr) {
		return bytesToUtf8(arr).replace("\0", "");
	}

	byte[] generateHomogenisedMessage(byte[] message);

	default void handleTransactionResponse(Map<String, byte[]> bodyMap) {

	}

	/**
	 * 
	 * @param response The homogenised response that needs to be handled
	 * @return <b>String</b>
	 * @throws IOException 
	 * @throws PortInUseException Should never happen, as only ports already open can trigger this function.
	 * @throws SerialPortInvalidPortException Should never happen, as only ports already open can trigger this function.
	 * @throws NeedToRestartPaymentException The transaction need to be restarted
	 * @throws DeviceException The device returned a failure response.
	 */
	default void handleResponse(byte[] response, PaymentDevice paymentDevice, ListeningService listeningService) throws SerialPortInvalidPortException, PortInUseException, IOException, NeedToRestartPaymentException, DeviceException {
		String respCode = bytesToUtf8(Arrays.copyOfRange(response, 14, 16));

		switch(respCode) {
		case ResponseCode.APPROVED:
			return;

		case ResponseCode.SETTLEMENT_REQUIRED:
			performSettlements(paymentDevice, listeningService);
			throw new NeedToRestartPaymentException("Restart Payment", "The transaction needs to be restarted. Please wait until the terminal is ready, and try again.");

		case ResponseCode.CANCELLED_BY_USER:
		case ResponseCode.UNABLE_TO_READ_CARD:
		case ResponseCode.INVALID_CARD1:
		case ResponseCode.INVALID_CARD2:
		case ResponseCode.INVALID_CARD3:
		case ResponseCode.INVALID_CARD4:
		case ResponseCode.INVALID_CARD5:
		case ResponseCode.INVALID_CARD6:
		case ResponseCode.INVALID_CARD7:
		case ResponseCode.AUTHORISATATION_REQUEST_DECLINED:
		case ResponseCode.TIMEOUT:
			throw new DeviceException("Transaction declined", "The transaction was declined. Please try again.");

		case ResponseCode.TERMINAL_IN_PRE_ACTIVATION:
		case ResponseCode.FAULTY_IUC:
		case ResponseCode.FAULTY_IUR:
		case ResponseCode.CALLBACK_DISPLAY_MESSAGE:
			throw new DeviceException("Problem with terminal", "There is an issue with the terminal. Please contact NETS.");

		case ResponseCode.TERMINAL_CONNECTION_PROBLEM:
			throw new DeviceException("Terminal connection problem", "Make sure the terminal is connected to the internet (Ethernet, SIM Card etc.)");
			
		case ResponseCode.OUT_OF_PAPER:
			throw new DeviceException("Terminal out of paper", "The terminal is out of paper. Please add another roll.");

		case ResponseCode.LOGON_REQUIRED:
			logonToDevice(paymentDevice, listeningService);
			throw new NeedToRestartPaymentException("Restart Payment", "The transaction needs to be restarted. Please wait until the terminal is ready, and try again.");

		case ResponseCode.THIRD_PARTY_APP_NOT_AVAILABLE:
			throw new DeviceException("Third-party app not available", "Check with the terminal provider on application loading.");

		case ResponseCode.MANDATORY_FIELD_CODE_MISSING:
			throw new DeviceException("Mandatory field code missing", "Check the message being sent to the terminal");

		case ResponseCode.FUNCTION_NOT_AVAILABLE:
			throw new DeviceException("Function not available", "The function specified does not exist.");

		default:
			throw new DeviceException("Unknown response", "Unknown response while requesting terminal status: `" + respCode + "`. Please contact admin.");
		}
	}

	default Codes getPaymentCodes(PaymentMethod method) throws DataException {
		switch(method) {
		case NETS:
			return Codes.nets(FunctionCode.NETS_PURCHASE);

		case CREDIT_CARD:
			return Codes.nets(FunctionCode.CREDIT_CARD_PURCHASE);

		case UNIONPAY:
			return Codes.nets(FunctionCode.UPI_PURCHASE);

		case BCA:
			return Codes.nets(FunctionCode.BCA_PURCHASE);

		case RUPAY:
			return Codes.nets(FunctionCode.RUPAY_PURCHASE);

		case MYDEBIT:
			return Codes.nets(FunctionCode.MYDEBIT_PURCHASE);

		case DEBIT_CARD:
			return Codes.uob("Pending");
		default:
			throw new DataException("Invalid Payment Type", "The Payment Type provided was not in the list. Please contact an admin.");
		}
	}


	default String getPaymentMethodFromFunctionCode(String functionCode) {
		switch(functionCode) {
		case FunctionCode.NETS_PURCHASE:
			return "NETS";

		case FunctionCode.CREDIT_CARD_PURCHASE:
			return "CREDIT_CARD";

		case FunctionCode.UPI_PURCHASE:
			return "UNIONPAY";

		case FunctionCode.BCA_PURCHASE:
			return "BCA";

		case FunctionCode.RUPAY_PURCHASE:
			return "RUPAY";

		case FunctionCode.MYDEBIT_PURCHASE:
			return "MYDEBIT";

		default:
			return "UNKNOWN";

		}
	}
}

