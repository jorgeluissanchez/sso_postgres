package com.co.lowcode.sso.exception;

public class EndpointDuplicateException extends BaseWebApplicationException {
	
private static final long serialVersionUID = -5613551094180513790L;
	
	
	public EndpointDuplicateException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}

	
	public EndpointDuplicateException(String name){
		
		super(401,"Cambie el nombre, el ("+name+") está en uso","El Endpoint ya se encuentra registrado");
	
	}
	
	

}
