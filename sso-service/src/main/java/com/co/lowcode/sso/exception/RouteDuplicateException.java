package com.co.lowcode.sso.exception;

public class RouteDuplicateException extends BaseWebApplicationException {
	private static final long serialVersionUID = -5613551094180513790L;
	
	
	public RouteDuplicateException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}

	
	public RouteDuplicateException(String name){
		
		super(401,"Cambie el nombre, el ("+name+") está en uso","La ruta de menú ya se encuentra registrada");
	
	}
	
}
