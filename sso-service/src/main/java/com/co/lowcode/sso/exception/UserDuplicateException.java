package com.co.lowcode.sso.exception;



public class UserDuplicateException extends BaseWebApplicationException {
	
	private static final long serialVersionUID = -5613551094180513790L;
	
	
	public UserDuplicateException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}


	public UserDuplicateException(String email){
		super(401,"Cambie su correo electrónico, el "+email+" está en uso","La cuenta de usuario se encuentra registrada");
	}
	
}
