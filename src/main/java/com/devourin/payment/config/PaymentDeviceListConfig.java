package com.devourin.payment.config;

import java.net.URISyntaxException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import com.devourin.payment.bean.PaymentDeviceList;
import com.devourin.payment.model.PaymentDevice;
import com.devourin.payment.util.EnvironmentUtil;

@Configuration
public class PaymentDeviceListConfig {

	@Autowired
	private RestTemplate restTemplate;

	@Bean
	public PaymentDeviceList paymentDeviceList() throws URISyntaxException {
		String baseUrl = EnvironmentUtil.getServerUrl();
		String terminalId = EnvironmentUtil.getTerminalId();

		List<PaymentDevice> deviceList = restTemplate.exchange(
				baseUrl + "/payment/getDeviceList?terminalId=" + terminalId,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<List<PaymentDevice>>() {}
				).getBody();

		return new PaymentDeviceList(deviceList);
	}

}
