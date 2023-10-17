package com.devourin.payment.model;

// Also change com.innobliss.nebula.print.pojo.ReturnedPaymentInfo in nebula

public class ReturnedPaymentInfo {
	// This field is only used to differentiate between new payments and previous payments.
	private boolean isPreviousPayment;
	
	private String responseText;
	private String date;
	private String time;
	private String deviceId;
	private int transactionAmount;
	private String stan;
	private String approvalCode;
	private String paymentMethod;
	private String cardType;

	public boolean isPreviousPayment() {
		return isPreviousPayment;
	}
	public void setPreviousPayment(boolean isPreviousPayment) {
		this.isPreviousPayment = isPreviousPayment;
	}
	public String getResponseText() {
		return responseText;
	}
	public void setResponseText(String responseText) {
		this.responseText = responseText;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public int getTransactionAmount() {
		return transactionAmount;
	}
	public void setTransactionAmount(int transactionAmount) {
		this.transactionAmount = transactionAmount;
	}
	public String getStan() {
		return stan;
	}
	public void setStan(String stan) {
		this.stan = stan;
	}
	public String getApprovalCode() {
		return approvalCode;
	}
	public void setApprovalCode(String approvalCode) {
		this.approvalCode = approvalCode;
	}
	public String getPaymentMethod() {
		return paymentMethod;
	}
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	public String getCardType() {
		return cardType;
	}
	public void setCardType(String cardType) {
		this.cardType = cardType;
	}
}
