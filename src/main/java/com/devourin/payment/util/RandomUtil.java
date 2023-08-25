package com.devourin.payment.util;

import java.time.Instant;
import java.util.Random;

public class RandomUtil {

	public static String generateRandomAlphanumericStringUppercase(int length) {
		int leftLimit = 48; // numeral '0'
	    int rightLimit = 90; // letter 'Z'
	    Random random = new Random();
	    
	    String generatedString = random.ints(leftLimit, rightLimit + 1)
	    		.filter(i -> (i <= 57 || i >= 65))
	    	    .limit(length)
	    	    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
	    	    .toString();

	    return generatedString;
	}
	
	public static String generateRandomIdWithCurrentTime(int charactersOnEnd) {
		return Long.toString(Instant.now().getEpochSecond())
				+ generateRandomAlphanumericStringUppercase(charactersOnEnd);
		
	}
}
