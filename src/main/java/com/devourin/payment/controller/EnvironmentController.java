package com.devourin.payment.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devourin.payment.util.EnvironmentUtil;

@RestController
@RequestMapping("/api")
public class EnvironmentController {

	/*
	 *                      ========== GUIDELINES ==========
	 * DO NOT MAKE THE ENVIRONMENT FUNCTIONS GENERIC. IT IS A HUGE SECURITY FLAW.
	 * ALWAYS CREATE YOUR OWN ENV VARIABLES.
	 * DO NOT ACCESS THE SYSTEM VARIABLES.
	 * DO NOT ACCESS SYSTEM VARIABLES THROUGH YOUR OWN ENV VARIABLES.
	 * DO NOT EXPOSE SENSITIVE DATA.
	 */
	@GetMapping("/v1/env/terminalId")
	public ResponseEntity<Map<String, String>> getTerminalId() {
		Map<String, String> resp = Map.of("terminalId", EnvironmentUtil.getTerminalId());
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/v1/env/serverUrl")
	public ResponseEntity<Map<String, String>> getServerUrl() {
		Map<String, String> resp = Map.of("serverUrl", EnvironmentUtil.getServerUrl());
		return ResponseEntity.ok(resp);
	}
}
