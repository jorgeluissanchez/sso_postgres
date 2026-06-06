package com.co.lowcode.sso.exception;

public class TokenExpiredException extends BaseWebApplicationException {
	public TokenExpiredException(){
		super(401,"El token ha expirado","El token ha expirado");
	}
}
