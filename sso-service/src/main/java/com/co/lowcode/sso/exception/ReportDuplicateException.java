package com.co.lowcode.sso.exception;

public class ReportDuplicateException extends BaseWebApplicationException {
	private static final long serialVersionUID = -5613551094180513790L;
	
	
	public ReportDuplicateException(int httpStatus, String errorMessage, String developerMessage) {
		super(httpStatus, errorMessage, developerMessage);
		// TODO Auto-generated constructor stub
	}

	
	public ReportDuplicateException(String name){
		
		super(401,"Cambie el nombre, el ("+name+") está en uso","El reporte ya se encuentra registrado");
	
	}
	
}
