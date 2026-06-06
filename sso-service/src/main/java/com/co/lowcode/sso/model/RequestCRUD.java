package com.co.lowcode.sso.model;

import java.util.List;

public class RequestCRUD {
	
	private String tableName;
	private List<String> attributes;
	private List<String> where;
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public List<String> getAttributes() {
		return attributes;
	}
	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}
	public List<String> getWhere() {
		return where;
	}
	public void setWhere(List<String> where) {
		this.where = where;
	}


}
