package com.co.lowcode.backend.model;

public class RequestQuery {
	private String uuid;
	private Object params;
	private String username;
	private Object where;
	private String captcha;
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public Object getParams() {
		return params;
	}
	public void setParams(Object params) {
		this.params = params;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Object getWhere() {
		return where;
	}
	public void setWhere(Object where) {
		this.where = where;
	}
	public String getCaptcha() {
		return captcha;
	}
	public void setCaptcha(String captcha) {
		this.captcha = captcha;
	}
}