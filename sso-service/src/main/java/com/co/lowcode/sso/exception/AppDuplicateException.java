package com.co.lowcode.sso.exception;

public class AppDuplicateException extends BaseWebApplicationException {
	private static final long serialVersionUID = -5613551094180513790L;
	
	
	public AppDuplicateException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}

	
	public AppDuplicateException(String name){
		
		super(401,"Cambie el nombre, el ("+name+") está en uso","La aplicación ya se encuentra registrada");
	
	}
	
}
