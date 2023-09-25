package com.devourin.payment.util;

import java.time.Instant;
import java.util.Random;

public class RandomUtil {
	private RandomUtil() {}
	private static final Random random = new Random();

	public static String generateRandomAlphanumericStringUppercase(int length) {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 90; // letter 'Z'

		return random.ints(leftLimit, rightLimit + 1)
				.filter(i -> (i <= 57 || i >= 65))
				.limit(length)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}

	public static String generateRandomIdWithCurrentTime(int charactersOnEnd) {
		return Long.toString(Instant.now().getEpochSecond())
				+ generateRandomAlphanumericStringUppercase(charactersOnEnd);

	}
}
