package com.co.lowcode.sso.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Brick {
	private String type;
	private String styleId;
	private String styleClasses;
	private List<String> children;
	
	public List<String> getChildren() {
		return children;
	}
	
	public void setChildren(List<String> children) {
		this.children = children;
	}
	public String getStyleClasses() {
		return styleClasses;
	}
	public void setStyleClasses(String styleClasses) {
		this.styleClasses = styleClasses;
	}
	public String getStyleId() {
		return styleId;
	}
	public void setStyleId(String styleId) {
		this.styleId = styleId;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}