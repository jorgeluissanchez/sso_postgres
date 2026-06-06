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

import com.co.lowcode.sso.service.AppAdmonService;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@RestController
public class AppAdmonController {
	
	@Autowired
	AppAdmonService appService;

	@RequestMapping(value = "/app/createApp", method = RequestMethod.POST)
	public Map<String,Object> createApp(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return appService.createApp(model);
	}
	
	
	@RequestMapping(value = "/app/updateApp", method = RequestMethod.PUT)
	public Map<String,Object> updateApp(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return appService.updateApp(model);
	}
	
	@GetMapping("/app/getApplications")
    public  List<Map<String,Object>> getApplications(  HttpServletRequest request)  {
		return appService.getApplications();
    }

	
	@GetMapping("/app/microservices")
    public  List<Map<String,Object>> getMicrocervicesApp(@RequestParam String appId,  HttpServletRequest request)  {
		return appService.getMicroservicesApp(appId);
    }
	
	@GetMapping("/app/microservices/checked")
    public  List<Map<String,Object>> getMicroservicesAppChecked(@RequestParam String appId,  HttpServletRequest request)  {
		return appService.getMicroservicesAppChecked(appId);
    }

	
	@GetMapping("/app/routes")
    public  List<Map<String,Object>> getRoutesApp(@RequestParam String appId,  HttpServletRequest request)  {
		return appService.getRoutesApp(appId);
    }
	
	@GetMapping("/app/routes/checked")
    public  List<Map<String,Object>> getRoutesAppChecked(@RequestParam String appId,  HttpServletRequest request)  {
		return appService.getRoutesAppChecked(appId);
    }
	
	
	@GetMapping("/app/roles/checked")
    public  List<Map<String,Object>> getRolesAppChecked( @RequestParam String appId, HttpServletRequest request)  {
		return appService.getRolesAppChecked(appId);
    }

	
	@RequestMapping(value = "/app/addMicroservice", method = RequestMethod.POST)
	public Map<String,Object> addMicroservice(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return appService.addMicroservice(model);
	}
	
	
	@RequestMapping(value = "/app/addRole", method = RequestMethod.POST)
	public Map<String,Object> addRole(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return appService.addRole(model);
	}
	
	
	@RequestMapping(value= "/app/removeMicroservice", method = RequestMethod.DELETE)
	public void removeMicroservice(@RequestParam String appId,@RequestParam String msId , HttpServletRequest request) throws SQLException {
		 
		appService.removeMicroservice(appId, msId);
	}
	
	@RequestMapping(value= "/app/removeRole", method = RequestMethod.DELETE)
	public void removeRole(@RequestParam String appId,@RequestParam String roleId , HttpServletRequest request) throws SQLException {
		 
		appService.removeRole(appId, roleId);
	}
	

	@RequestMapping(value = "/app/addRoute", method = RequestMethod.POST)
	public Map<String,Object> addRoute(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return appService.addRoute(model);
	}
	
	
	@RequestMapping(value= "/app/removeRoute", method = RequestMethod.DELETE)
	public void removeRoute(@RequestParam String appId,@RequestParam String routeId , HttpServletRequest request) throws SQLException {
		 
		appService.removeRoute(appId, routeId);
	}
	

	
	@RequestMapping(value = "/app/saveRoutes", method = RequestMethod.POST)
	public List< Map<String,Object>> saveRoutes(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		
		String roleId= model.get("roleId").toString();
		String routes = model.get("routes").toString();
		appService.saveRoutes(roleId, routes);
		
		return appService.getRoutesAppChecked(roleId);
		
	}

	

}
