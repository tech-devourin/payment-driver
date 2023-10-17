package com.devourin.payment.bean;

import java.util.List;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.util.EnvironmentUtil;

@Configuration
public class PaymentDeviceListBean {

	@Bean
	public List<PaymentDevice> paymentDeviceList(RestTemplate restTemplate) throws InterruptedException {
		String baseUrl = EnvironmentUtil.getServerUrl();
		String terminalId = EnvironmentUtil.getTerminalId();

		Throwable cause = null;

		// 5 tries 5 seconds apart, just in case the main application takes some time to start
		for(int i = 0; i < 5;) {
			try {
				return restTemplate.exchange(
						baseUrl + "/payment/getDeviceList?terminalId=" + terminalId,
						HttpMethod.GET,
						null,
						new ParameterizedTypeReference<List<PaymentDevice>>() {}
						).getBody();
			} catch (RestClientException e) {
				cause = e;
				Thread.sleep(5000);
			}
		}

		String message = "There was an error while getting the list of payment devices.";
		if(cause == null) throw new BeanInitializationException(message);
		else throw new BeanInitializationException(message, cause);
		
	}

}
