package com.co.lowcode.lineabase.oauth.model;

import java.util.List;

public class LoginResponse {

	private List<RouteResponse> routes;
	private String token;
	private String refreshToken;
	
	public List<RouteResponse> getRoutes() {
		return routes;
	}
	public void setRoutes(List<RouteResponse> routes) {
		this.routes = routes;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	
	
	
}
