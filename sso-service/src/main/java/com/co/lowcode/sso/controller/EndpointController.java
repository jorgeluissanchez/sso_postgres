package com.co.lowcode.sso.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.sso.service.EndpointService;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;


@RestController
public class EndpointController {
	
	
	@Autowired
	EndpointService endpointService;

	@RequestMapping(value = "/endpoint/createEndpoint", method = RequestMethod.POST)
	public Map<String,Object> createEndpoint(@RequestBody Map<String, Object> endpoint) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return endpointService.createEndpoint(endpoint);
	}
	
	
	@RequestMapping(value = "/endpoint/updateEndpoint", method = RequestMethod.PUT)
	public Map<String,Object> updateEndpoint(@RequestBody Map<String, Object> endpoint) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return endpointService.updateEndpoint(endpoint);
	}
	 
 	
	@GetMapping("/endpoint/getEndpoints")
    public  List<Map<String,Object>> getEndpoints(  HttpServletRequest request)  {
		return endpointService.getEndpoints();
    }
	
	
	@GetMapping("/endpoint/roles")
    public  List<Map<String,Object>> getRolesEndpoint( @RequestParam String endpointId, HttpServletRequest request)  {
		return endpointService.getRolesEndpoint(endpointId);
    }
	
	
	@GetMapping("/endpoint/roles/checked")
    public  List<Map<String,Object>> getRolesEndpointChecked( @RequestParam String endpointId, HttpServletRequest request)  {
		return endpointService.getRolesEndpointChecked(endpointId);
    }
	
	
	@GetMapping("/endpoint/microservices")
    public  List<Map<String,Object>> getMicroservicesEndpoint(@RequestParam String endpointId,  HttpServletRequest request)  {
		return endpointService.getMicroservicesEndpoint(endpointId);
    }
	
	@GetMapping("/endpoint/microservices/checked")
    public  List<Map<String,Object>> getMicroservicesEndpointChecked(@RequestParam String endpointId,  HttpServletRequest request)  {
		return endpointService.getMicroservicesEndpointChecked(endpointId);
    }

}
