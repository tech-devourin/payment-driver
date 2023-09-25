package com.devourin.payment.bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.devourin.payment.constant.Model;

@Configuration
public class PaymentIdMapBean {

	@Bean
	Map<Model, Map<String, Long>> paymentIdMap() {
		Map<Model, Map<String, Long>> map = new ConcurrentHashMap<>();
		Model[] keys = Model.values();
		
		for(int i = 0; i < keys.length; i++) {
			map.put(keys[i], new ConcurrentHashMap<>());
		}
		return map;
	}
}
