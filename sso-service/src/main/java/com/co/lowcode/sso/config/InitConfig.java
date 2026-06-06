package com.co.lowcode.sso.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.co.lowcode.sso.service.UserService;

@Component
public class InitConfig implements ApplicationRunner {

	

	@Value("${username}")
	private String username;
	
	@Value("${fullname}")
	private String fullname;
	
	
	@Autowired
	private UserService userService;
	
	
	public void run(ApplicationArguments args) throws Exception {
		try {
			
			if(username != null) {
				Map<String, Object> user = new HashMap<>();
				user.put("USERNAME", username);
				user.put("FULLNAME", fullname);
				user.put("ID_ROLE", 1);
				userService.createAccount(user, "", "");
			}
			
		} catch (Exception e) {
			System.out.println(e);
		}
		
	}
	

}
