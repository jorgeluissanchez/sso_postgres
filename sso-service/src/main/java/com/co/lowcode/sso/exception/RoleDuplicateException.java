package com.co.lowcode.sso.exception;

public class RoleDuplicateException extends BaseWebApplicationException {
	private static final long serialVersionUID = -5613551094180513790L;
	
	
	public RoleDuplicateException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}

	
	public RoleDuplicateException(String name){
		
		super(401,"Cambie el nombre, el ("+name+") está en uso","El role ya se encuentra registrado");
	
	}
	
}
