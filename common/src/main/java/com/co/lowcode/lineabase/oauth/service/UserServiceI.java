package com.co.lowcode.lineabase.oauth.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.co.lowcode.lineabase.model.Role;
import com.co.lowcode.lineabase.model.User;
import com.co.lowcode.lineabase.oauth.model.LoginResponse;
import com.co.lowcode.lineabase.oauth.model.RouteResponse;

public interface UserServiceI {
	List<RouteResponse> findOpcionByUsernameAndApp(String username,String app_name);
	Set<Role> findRoleByUsername(String username);
	User getUserByUserName(String username);
	UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
	void saveRefreshToken(String login, String refreshToken);
	LoginResponse getToken(HttpServletRequest request, String refreshToken, String appName) throws Exception;
	
	List<RouteResponse> findOpcionByApiTokenAndApp(String apiToken, String app_name);
	
	LoginResponse getApiToken(String apiToken, String appName) throws IOException;
	User getUserByUserNameLocal(String username);
	List<Map<String, Object>> getUsersSSO();
	LoginResponse googleLogin(String idTokenString, String appName);
	UserDetails loadUserByUsername(User user) throws UsernameNotFoundException;
}
