package com.devourin.payment.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devourin.payment.util.EnvironmentUtil;

@RestController
@RequestMapping("/devourin-payment/v1/env")
public class EnvironmentController {

	/*
	 *                      ========== GUIDELINES ==========
	 * DO NOT MAKE THE ENVIRONMENT FUNCTIONS GENERIC. IT IS A HUGE SECURITY FLAW.
	 * ALWAYS CREATE YOUR OWN ENV VARIABLES.
	 * DO NOT ACCESS THE SYSTEM VARIABLES.
	 * DO NOT ACCESS SYSTEM VARIABLES THROUGH YOUR OWN ENV VARIABLES.
	 * DO NOT EXPOSE SENSITIVE DATA.
	 */
	@GetMapping("/terminalId")
	public ResponseEntity<?> getTerminalId() {
		Map<String, String> resp = Map.of("terminalId", EnvironmentUtil.getTerminalId());
		return ResponseEntity.ok(resp);
	}
	
	@GetMapping("/serverUrl")
	public ResponseEntity<?> getServerUrl() {
		Map<String, String> resp = Map.of("serverUrl", EnvironmentUtil.getServerUrl());
		return ResponseEntity.ok(resp);
	}
}
