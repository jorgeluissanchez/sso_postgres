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

import com.co.lowcode.sso.service.RouteService;

 
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RouteController {
	
	@Autowired
	RouteService rs;

	@RequestMapping(value = "/route/createRoute", method = RequestMethod.POST)
	public Map<String,Object> createRoute(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.createRoute(model);
	}
	
	
	@RequestMapping(value = "/route/updateRoute", method = RequestMethod.PUT)
	public Map<String,Object> updateRoute(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.updateRoute(model);
	}
	
	@GetMapping("/route/getRoutes")
    public  List<Map<String,Object>> geRoutes(  HttpServletRequest request)  {
		return rs.getRoutes();
    }

	@GetMapping("/route/routes")
    public  List<Map<String,Object>> getRoutesByParent(@RequestParam String parentId,  HttpServletRequest request)  {
		return rs.getRoutesByParent(parentId);
    }

	
	@GetMapping("/route/roles/checked")
    public  List<Map<String,Object>> getRolesRouteChecked(@RequestParam String routeId,  HttpServletRequest request)  {
		return rs.getRolesRouteChecked(routeId);
    }

	@GetMapping("/route/roles")
    public  List<Map<String,Object>> getRolesRoute(@RequestParam String routeId,  HttpServletRequest request)  {
		return rs.getRolesRoute(routeId);
    }


	@GetMapping("/route/apps/checked")
    public  List<Map<String,Object>> getAppsRouteChecked(@RequestParam String routeId,  HttpServletRequest request)  {
		return rs.getAppsRouteChecked(routeId);
    }

	@GetMapping("/route/apps")
    public  List<Map<String,Object>> getAppsRoute(@RequestParam String routeId,  HttpServletRequest request)  {
		return rs.getAppsRoute(routeId);
    }
	 
	
	@RequestMapping(value = "/route/addApp", method = RequestMethod.POST)
	public Map<String,Object> addApp(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.addApp(model);
	}
	
	
	@RequestMapping(value= "/route/removeApp", method = RequestMethod.DELETE)
	public void removeApp(@RequestParam String routeId,@RequestParam String appId , HttpServletRequest request) throws SQLException {
		 
	  rs.removeApp(routeId, appId);
	}
	

	
	@RequestMapping(value = "/route/addRole", method = RequestMethod.POST)
	public Map<String,Object> addRole(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.addRole(model);
	}
	
	
	@RequestMapping(value= "/route/removeRole", method = RequestMethod.DELETE)
	public void removeRole(@RequestParam String routeId,@RequestParam String roleId , HttpServletRequest request) throws SQLException {
		 
	  rs.removeRole(routeId, roleId);
	}
	

	
}
