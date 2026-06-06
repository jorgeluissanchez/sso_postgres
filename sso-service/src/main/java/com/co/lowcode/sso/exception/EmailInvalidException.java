package com.co.lowcode.sso.exception;



public class EmailInvalidException extends BaseWebApplicationException {
	
	private static final long serialVersionUID = -5613551094180513790L;
	
	
	public EmailInvalidException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}


	public EmailInvalidException(){
		super(401,"Correo electrónico invalido","Correo electrónico invalido");
	}
	
}
