package com.devourin.payment.controller;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devourin.payment.bean.PaymentIdMapBean;
import com.devourin.payment.constant.Model;
import com.devourin.payment.dto.PaymentInfoDto;
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

@RestController
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
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

	@GetMapping("/v1/status")
	public void checkModule() {
		// This is for checking if the payment module is installed.
	}

	// The mapping for this message API is `/app/ws/v1/payment`
	@MessageMapping("/v1/payment")
	public void createPayment(PaymentInfoDto body) {
		try {
			PaymentInfo paymentInfo = convertDtoToModel(body);

			boolean isFree = false;

			for(int i = 0; i < paymentDeviceList.size() && !isFree; i++) {
				isFree = createPayment(paymentDeviceList.get(i), paymentInfo); 
			}

			// Was not able to complete
			if(!isFree) {
				throw new CouldNotProcessException("None of the devices connected to this terminal are free");
			}

		} catch (DataException e) {
			paymentMessagingService.sendWebsocketDevourinError(e);
		} catch (Exception e) {
			paymentMessagingService.sendWebsocketError(e);
		}
	}

	private boolean createPayment(PaymentDevice paymentDevice, PaymentInfo body) throws IOException, DataException {
		try {
			switch(paymentDevice.getModel()) {
			case NETS:
				NetsService netsService = getNetsService(paymentDevice);
				netsService.createPayment(body, paymentDevice, listeningService);
			}

			return true;

		} catch (PortInUseException e) {
			// Goes to the next iteration to check for a free device
			return false;
		}
	}

	/**
	 * Runs every hour to clean up {@link PaymentIdMapBean#paymentIdMap}
	 */
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

	private PaymentInfo convertDtoToModel(PaymentInfoDto paymentInfoDto) throws DataException {
		if(paymentInfoDto == null) {
			throw new DataException("Missing body", "Please provide payment information.");
		}

		if(paymentInfoDto.getAmount() == null || paymentInfoDto.getAmount() <= 0) {
			throw new DataException("Missing payment value", "Please send the amount to pay");
		}

		try { // Checking for a valid PaymentMethod
			return new PaymentInfo(paymentInfoDto);
		} catch (NullPointerException | IllegalArgumentException e) {
			throw new DataException("Invalid Payment Type", "The payment type provided was not in the list. Please contact an admin.");
		}
	}

	private NetsService getNetsService(PaymentDevice paymentDevice) throws DataException {
		switch(paymentDevice.getProtocol()) {
		case RS232:
			return netsRs232Service;

		case ETHERNET:
			//			return netsServiceEthernet;
		default:
			throw new DataException("Invalid Device Protocol", "The protocol for the device is invalid. Please contact your server admin.");
		}
	}

}
