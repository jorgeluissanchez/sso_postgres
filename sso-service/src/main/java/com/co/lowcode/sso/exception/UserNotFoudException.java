package com.co.lowcode.sso.exception;



public class UserNotFoudException extends BaseWebApplicationException {
	
	private static final long serialVersionUID = -5613551094180513790L;
	
	
	public UserNotFoudException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}


	public UserNotFoudException(String email){
		super(401,"La cuenta de usuario  "+email+" no se encuentra registrada","La cuenta de usuario no se encuentra registrada");
	}
	
}
