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

import com.co.lowcode.sso.service.MicroserviceService;

import com.co.lowcode.sso.util.Util;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@RestController
public class MicroserviceController {

	@Autowired
	MicroserviceService ms;


	@RequestMapping(value = "/microservice/createInitMicroservice", method = RequestMethod.POST)
	public Map<String,Object> createInitMicroservice(@RequestBody Map<String, Object> microservice) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return ms.createInitMicroservice(microservice);
	}
	
	
	
	@RequestMapping(value = "/microservice/createMicroservice", method = RequestMethod.POST)
	public Map<String,Object> createMicroservice(@RequestBody Map<String, Object> microservice) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return ms.createMicroservice(microservice);
	}
	
	
	@RequestMapping(value = "/microservice/updateMicroservice", method = RequestMethod.PUT)
	public Map<String,Object> updateMicroservice(@RequestBody Map<String, Object> microservice) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return ms.updateMicroservice(microservice);
	}
	
	@GetMapping("/microservice/getMicroservices")
    public  List<Map<String,Object>> getMicroservices(HttpServletRequest request)  {
		return ms.getMicroservices();
    }
	
	@GetMapping("/microservice/getMicroservice")
    public  List<Map<String,Object>> getMicroservices(@RequestParam String serviceId,HttpServletRequest request)  {
		return ms.getMicroservice(serviceId);
    }
	
	@GetMapping("/microservice/endpoints")
    public  List<Map<String,Object>> getEndpointsMicroservice(@RequestParam String msId,  HttpServletRequest request)  {
		return ms.getEndpointsMicroservice(msId);
    }

	
	@GetMapping("/microservice/endpoints/checked")
    public  List<Map<String,Object>> getEndpointsMicroserviceChecked(@RequestParam String msId,  HttpServletRequest request)  {
		return ms.getEndpointsMicroserviceChecked(msId);
    }
	
	@GetMapping("/microservice/apps")
    public  List<Map<String,Object>> getAppsMicroservice(@RequestParam String msId,  HttpServletRequest request)  {
		return ms.getAppsMicroservice(msId);
    }

	
	@GetMapping("/microservice/apps/checked")
    public  List<Map<String,Object>> getAppsMicroserviceChecked(@RequestParam String msId,  HttpServletRequest request)  {
		return ms.getAppsMicroserviceChecked(msId);
    }


	
	@RequestMapping(value = "/microservice/addEndpoint", method = RequestMethod.POST)
	public Map<String,Object> addEndpoint(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return ms.addEndpoint(model);
	}
	
	
	@RequestMapping(value= "/microservice/removeEndpoint", method = RequestMethod.DELETE)
	public void removeEndpoint(@RequestParam String msId,@RequestParam String endpointId , HttpServletRequest request) throws SQLException {
		 
	  ms.removeEndpoint(msId, endpointId);
	}
	
	
	
	
}

