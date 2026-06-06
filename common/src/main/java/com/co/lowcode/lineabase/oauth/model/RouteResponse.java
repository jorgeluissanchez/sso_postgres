package com.co.lowcode.lineabase.oauth.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) 
public class RouteResponse {

	public RouteResponse() {
	}

	public Long idRoute;
	
	public Long idParent;
	
	public String nameRoute;
	
	public String path;
	
	public String type;
	
	public String componentId;
	
	public String icon;
	
	public Integer menuOrder;
	
	public Long getIdRoute() {
		return idRoute;
	}

	/**
	 * @param idOpcion the idOpcion to set
	 */
	public void setIdRoute(Long idRoute) {
		this.idRoute = idRoute;
	}

	/**
	 * @return the nombreOpcion
	 */
	public String getNameRoute() {
		return nameRoute;
	}

	/**
	 * @param nombreOpcion the nombreOpcion to set
	 */
	public void setNameRoute(String nameRoute) {
		this.nameRoute = nameRoute;
	}

	

	/**
	 * @return the ruta
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param ruta the ruta to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public Long getIdParent() {
		return idParent;
	}

	public void setIdParent(Long idParent) {
		this.idParent = idParent;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Integer getMenuOrder() {
		return menuOrder;
	}

	public void setMenuOrder(Integer menuOrder) {
		this.menuOrder = menuOrder;
	}

	

}
