package com.devourin.payment.controller;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devourin.payment.constant.Model;
import com.devourin.payment.exception.CouldNotProcessException;
import com.devourin.payment.exception.DataException;
import com.devourin.payment.exception.PortInUseException;
import com.devourin.payment.model.DevicePortMapping;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.model.PaymentInfo;
import com.devourin.payment.service.ListeningService;
import com.devourin.payment.service.NetsRs232Service;
import com.devourin.payment.service.NetsService;
import com.devourin.payment.service.PaymentMessagingService;
import com.devourin.payment.util.Rs232Util;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@RestController
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@RequestMapping("/api")
@MessageMapping("/ws") // Required for websockets
public class PaymentController {

	@Autowired
	NetsRs232Service netsRs232Service;

	// @Autowired
	//	NetsEthernetService netsEthernetService;

	@Autowired
	ListeningService listeningService;

	@Autowired
	List<PaymentDevice> paymentDeviceList;

	@Autowired
	Map<Model, Map<String, Long>> paymentIdMap;

	@Autowired
	Map<String, DevicePortMapping> openPorts;

	@Autowired
	PaymentMessagingService paymentMessagingService;

	// The mapping for this message API is `/app/ws/v1/payment`
	@MessageMapping("/v1/payment")
	public void createPayment(PaymentInfo body) {
		try {
			validatePaymentInfo(body);
			boolean isFree = false;

			for(int i = 0; i < paymentDeviceList.size() && !isFree; i++) {
				isFree = createPayment(paymentDeviceList.get(i), body, i); 
			}

			// Was not able to complete
			if(!isFree) {
				throw new CouldNotProcessException("None of the devices connected to this terminal were free");
			}

		} catch (DataException e) {
			paymentMessagingService.sendWebsocketDataError(e);
		} catch (Exception e) {
			paymentMessagingService.sendWebsocketError(e);
		}
	}

	private boolean createPayment(PaymentDevice paymentDevice, PaymentInfo body, int iteration) throws IOException, DataException {
		try {
			switch(paymentDevice.getModel()) {
			case NETS:
				NetsService netsService = getNetsService(paymentDevice, iteration);
				netsService.createPayment(body, paymentDevice, listeningService);
			}

			return true;

		} catch (PortInUseException e) {
			// Goes to the next iteration to check for a free device
			return false;
		}
	}

	public void login() {
		List<Map<String, String>> errors = new ArrayList<>();

		for(int i = 0; i < paymentDeviceList.size(); i++) {
			PaymentDevice paymentDevice = paymentDeviceList.get(i);

			try {

				switch(paymentDevice.getModel()) {
				case NETS:
					NetsService netsService = getNetsService(paymentDevice, i);
					netsService.callTMS(paymentDevice, listeningService);
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
	}

	@GetMapping("/tms")
	public ResponseEntity<Object> callTMS(@RequestBody PaymentDevice paymentDevice) throws SerialPortInvalidPortException, PortInUseException, IOException {
		try {
			byte[] message = {};

			switch(paymentDevice.getModel()) {
			case NETS:
				NetsService netsService = getNetsService(paymentDevice, 0);
				netsService.callTMS(paymentDevice, listeningService);
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
		if(body.getAmount() == null || body.getAmount() <= 0) {
			throw new DataException("Missing payment value", "Please send the amount to pay");
		}
		if(body.getPaymentMethod() == null) {
			throw new DataException("Missing payment method", "Please include the method of payment");
		}

	}

	private NetsService getNetsService(PaymentDevice paymentDevice, int index) throws DataException {
		switch(paymentDevice.getProtocol()) {
		case RS232:
			return netsRs232Service;

		case ETHERNET:
			//			return netsServiceEthernet;
		default:
			throw new DataException("Invalid Device Protocol", "The protocol for the device at index " + index + " is invalid. Please contact your server admin.");
		}
	}

	// Login to all the devices attached to make them ready.
	@EventListener
	public void onApplicationEvent(ApplicationReadyEvent event) {
		login();
	}

	/**
	 * Runs every hour to clean up {@link NetsService#ecnWaitingForResponse}
	 */
	@Async
	@Scheduled(fixedRate=1, initialDelay=1, timeUnit = TimeUnit.HOURS)
	void checkIdMap() {
		Model[] keys = Model.values();
		long currentTime = Instant.now().getEpochSecond();

		for(int i = 0; i < keys.length; i++) { // For every model
			Map<String, Long> ids = paymentIdMap.get(keys[i]);
			Set<String> idSet = ids.keySet();
			Iterator<String> it = idSet.iterator();
			while(it.hasNext()) {
				String id = it.next();
				Long time = ids.get(id);
				if(currentTime - time > 600_000) { // more than 10 minutes ago
					ids.remove(id);
				}
			}
		}
	}

}
