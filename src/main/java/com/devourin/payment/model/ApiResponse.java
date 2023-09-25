package com.devourin.payment.model;

public class ApiResponse {
	String status;
	Object error;
	Object data;
	
	private ApiResponse() {}
	
	public static ApiResponse generateError(String status, Object error) {
		ApiResponse resp = new ApiResponse();
		resp.status = status;
		resp.error = error;
		
		return resp;
	}
	
	public static ApiResponse generateData(String status, Object data) {
		ApiResponse resp = new ApiResponse();
		resp.status = status;
		resp.data = data;
		
		return resp;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Object getError() {
		return error;
	}
	public void setError(Object error) {
		this.error = error;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
}
