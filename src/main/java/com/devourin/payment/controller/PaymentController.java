package com.devourin.payment.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.devourin.payment.bean.PaymentDeviceList;
import com.devourin.payment.exception.DataException;
import com.devourin.payment.exception.PortInUseException;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.model.PaymentInfo;
import com.devourin.payment.service.NetsRs232Service;
import com.devourin.payment.service.NetsService;
import com.devourin.payment.util.Rs232Util;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@Controller
@RequestMapping("/payment")
@MessageMapping("/payment") // Required for websockets
public class PaymentController {

	@Autowired
	NetsRs232Service netsRs232Service;
	//	NetsEthernetService netsServiceEthernet = new NetsEthernetService();

	@Autowired
	SimpMessagingTemplate messagingTemplate;
	
	@Autowired
	PaymentDeviceList paymentDeviceList;

//  // The mapping for this message API is `/app/payment/test`
//	@MessageMapping("/test")
//	public void sendDetails(Map<String, String> msg) throws Exception {
//		messagingTemplate.convertAndSend("/terminal/queue/payment-user" + msg.get("to"), msg);
//	}


	@PostMapping("/createPayment")
	public ResponseEntity<?> createPayment(@RequestBody PaymentInfo body) {
		try {
			validatePaymentInfo(body);

			boolean notPaid = true;
			List<PaymentDevice> paymentDevices = paymentDeviceList.getList();

			for(int i = 0; i < paymentDevices.size() && notPaid; i++) {
				PaymentDevice paymentDevice = paymentDevices.get(i);

				try {
					switch(paymentDevice.getModel()) {
					case "NETS":
						NetsService netsService = getNetsService(paymentDevice, i);
						netsService.createPayment(body, paymentDevice);

						// Will not reach if there is an error in the createPayment function
						notPaid = false; // TODO
					}

				} catch (PortInUseException e) {
					e.printStackTrace();
					// Goes to the next iteration to check for a free device
				} catch (IOException e) {
					e.printStackTrace();
					return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("There was an issue while communicating with the device");
				}
			}

			if(notPaid) {
				return ResponseEntity.ok("There were no free payment devices for this terminal.");
			}

		} catch (DataException e) {
			return ResponseEntity.badRequest().body(e.toMap());
		}
		return ResponseEntity.ok(null);
	}

	@PostMapping("/login")
	public ResponseEntity<?> login() {
		List<Map<String, String>> errors = new ArrayList<>();
		List<PaymentDevice> paymentDevices = paymentDeviceList.getList();

		for(int i = 0; i < paymentDevices.size(); i++) {
			PaymentDevice paymentDevice = paymentDevices.get(i);

			try {
				switch(paymentDevice.getModel()) {
				case "NETS":
					NetsService netsService = getNetsService(paymentDevice, i);
					netsService.logonToDevice(paymentDevice);
					break;
				}

			} catch (PortInUseException e) {
				// This error indicates the terminal is already in use
				// Goes to the next iteration to check for a free device
			} catch (IOException | SerialPortInvalidPortException e) {
				Map<String, String> err = Map.of("message", e.getMessage());
				errors.add(err);
			} catch (DataException e) {
				Map<String, String> err = e.toMap();
				errors.add(err);
			}
		}
		return ResponseEntity.ok(errors);
	}

	@GetMapping("/tms")
	public ResponseEntity<?> callTMS(@RequestBody PaymentDevice paymentDevice) throws SerialPortInvalidPortException, PortInUseException, IOException {
		try {
			byte[] message = {};

			switch(paymentDevice.getModel()) {
			case "NETS":
				NetsService netsService = getNetsService(paymentDevice, 0);
				message = netsService.callTMS(paymentDevice);
				break;
			}

			return ResponseEntity.ok(Rs232Util.getHexString(message));

		} catch (PortInUseException | IOException | SerialPortInvalidPortException e) {
			Map<String, String> err = Map.of("message", e.getMessage());
			return ResponseEntity.badRequest().body(err);
		} catch (DataException e) {
			Map<String, String> err = e.toMap();
			return ResponseEntity.badRequest().body(err);
		}

	}

	private void validatePaymentInfo(PaymentInfo body) throws DataException {
		if(body == null) {
			throw new DataException("Missing body", "Please provide payment information.");
		}
		if(body.getAmount() == null) {
			throw new DataException("Missing payment value", "Please send the amount to pay");
		}
	}

	private NetsService getNetsService(PaymentDevice paymentDevice, int index) throws DataException {
		switch(paymentDevice.getProtocol()) {
		case "RS232":
			return netsRs232Service;

		case "Ethernet":
			//return netsServiceEthernet;
		default:
			throw new DataException("Invalid Device Protocol", "The protocol for the device at index " + index + " is invalid. Please contact your server admin.");
		}
	}
}
