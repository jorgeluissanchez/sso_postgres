package com.co.lowcode.sso.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Component {
	private String id;
	private List<String> children;
	private String styleClasses;
	private String styleId;
	private String type;
	private String screenId;
	private List<Map<String,Object>> state;
	private List<Map<String,Object>> channelListeners;
	private Map<String,Object> disabled;
	private List<Map<String,Object>> events;
	private List<Map<String,Object>> dataRef;
	private Map<String,Object> renderProps;
	private List<Map<String,Object>> input;
	private Map<String,Object> props;
	
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
	public String getScreenId() {
		return screenId;
	}
	public void setScreenId(String screenId) {
		this.screenId = screenId;
	}
	public List<Map<String, Object>> getState() {
		return state;
	}
	public void setState(List<Map<String, Object>> state) {
		this.state = state;
	}
	public List<Map<String, Object>> getChannelListeners() {
		return channelListeners;
	}
	public void setChannelListeners(List<Map<String, Object>> channelListeners) {
		this.channelListeners = channelListeners;
	}
	public Map<String, Object> getDisabled() {
		return disabled;
	}
	public void setDisabled(Map<String, Object> disabled) {
		this.disabled = disabled;
	}
	public List<Map<String, Object>> getEvents() {
		return events;
	}
	public void setEvents(List<Map<String, Object>> events) {
		this.events = events;
	}
	public List<Map<String, Object>> getDataRef() {
		return dataRef;
	}
	public void setDataRef(List<Map<String, Object>> dataRef) {
		this.dataRef = dataRef;
	}
	public Map<String, Object> getRenderProps() {
		return renderProps;
	}
	public void setRenderProps(Map<String, Object> renderProps) {
		this.renderProps = renderProps;
	}
	public List<Map<String, Object>> getInput() {
		return input;
	}
	public void setInput(List<Map<String, Object>> input) {
		this.input = input;
	}
	public Map<String, Object> getProps() {
		return props;
	}
	public void setProps(Map<String, Object> props) {
		this.props = props;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	

}
