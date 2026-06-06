package com.co.lowcode.audit.util;

import java.io.IOException;

import org.springframework.security.jwt.JwtHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public final class Util {

	public static String getUserName(String auth) {
		org.springframework.security.jwt.Jwt jwtToken = JwtHelper.decode(auth);
		String jwtClaims = jwtToken.getClaims();
		try {
			JsonNode jwtClaimsJsonNode = new ObjectMapper().readTree(jwtClaims);
			String username = jwtClaimsJsonNode.get("sub").asText();
			return username;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

}
