package com.devourin.payment.util;

import java.math.BigDecimal;

public class NumberUtil {
	private NumberUtil() {}
	
	public static String padNumberWithZeroes(int num, int minLength) {
		return padNumberWithZeroes((long) num, minLength);
	}
	public static String padNumberWithZeroes(long num, int minLength) {
		return padNumberWithZeroes(Long.toString(Math.abs(num)), num < 0, minLength);
		
	}
	public static String padNumberWithZeroes(BigDecimal num, int minLength) {
		return padNumberWithZeroes(num.abs().toPlainString(), num.compareTo(BigDecimal.ZERO) < 0, minLength);
	}
	
	private static String padNumberWithZeroes(String numStr, boolean negative, int minLength) {
		if(numStr.length() < minLength) {
			StringBuilder sb = new StringBuilder(minLength);
			for(int i = 0; i < minLength - numStr.length(); i++) {
				sb.append("0");
			}
			sb.append(numStr);
			numStr = sb.toString();
		}
		if(negative) {
			if(numStr.startsWith("0")) {
				numStr = numStr.replaceFirst("0", "-");
			} else {
				numStr = "-" + numStr;
			}
		}
		return numStr;
	}
}
