package com.devourin.payment.bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.devourin.payment.model.DevicePortMapping;

@Configuration
public class OpenPortsBean {

	@Bean
	Map<String, DevicePortMapping> openPorts() {
		return new ConcurrentHashMap<>();
	}
}
