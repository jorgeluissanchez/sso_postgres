package com.co.lowcode.sso.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.sso.service.ExternalEndpointService;
import com.co.lowcode.sso.util.Util;

@RestController
public class ExternalEndpointController {
	
	 @Autowired
	 ExternalEndpointService externalEndpointService;
	
	  @RequestMapping(value = "/getExternalEndpoint", method = RequestMethod.POST)
	    public  Object getExternalEndpoint(@RequestBody Map<String,Object> body, HttpServletRequest request) throws JSONException {
		  String username = Util.getUserName(request.getHeader("authorization").substring(7));
	       return externalEndpointService.getExternalEndpoint(body,username);
	    }

}
