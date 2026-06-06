package com.co.lowcode.sso.exception;

public class MicroserviceDuplicateException extends BaseWebApplicationException {
	
	public MicroserviceDuplicateException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}

	
	public MicroserviceDuplicateException(String name){
		
		super(401,"Cambie el nombre, el ("+name+") está en uso","El microservicio ya se encuentra registrado");
	
	}

}
