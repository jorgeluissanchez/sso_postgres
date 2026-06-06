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

import com.co.lowcode.sso.service.RoleService;
import com.co.lowcode.sso.util.Util;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@RestController
public class RoleController {
	
	@Autowired
	RoleService rs;

	@RequestMapping(value = "/role/createRole", method = RequestMethod.POST)
	public Map<String,Object> createRole(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.createRole(model);
	}
	
	
	@RequestMapping(value = "/role/updateRole", method = RequestMethod.PUT)
	public Map<String,Object> updateRole(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.updateRole(model);
	}
	
	@GetMapping("/role/getRoles")
    public  List<Map<String,Object>> getRoles(  HttpServletRequest request)  {
		return rs.getRoles();
    }
	
	@GetMapping("/role/getRolesOwn")
    public  List<Map<String,Object>> getRolesOwn(  HttpServletRequest request)  {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return rs.getRolesOwn(username);
    }

	
	@GetMapping("/role/users")
    public  List<Map<String,Object>> getUsersRole(@RequestParam String roleId,  HttpServletRequest request)  {
		return rs.getUsersRole(roleId);
    }
	
	@GetMapping("/role/users/checked")
    public  List<Map<String,Object>> getUsersRoleChecked(@RequestParam String roleId,  HttpServletRequest request)  {
		return rs.getUsersRoleChecked(roleId);
    }
	
	@GetMapping("/up/roles/checked")
    public  List<Map<String,Object>> getRolesUserChecked(@RequestParam String userId,  HttpServletRequest request)  {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return rs.getRolesUserChecked(userId, username);
    }


	
	@GetMapping("/role/endpoints")
    public  List<Map<String,Object>> getEndpointsRole(@RequestParam String roleId,  HttpServletRequest request)  {
		return rs.getEndpointsRole(roleId);
    }

	@GetMapping("/role/endpoints/checked")
    public  List<Map<String,Object>> getEndpointsRoleChecked(@RequestParam String roleId,  HttpServletRequest request)  {
		return rs.getEndpointsRoleChecked(roleId);
    }

	
	@GetMapping("/role/routes")
    public  List<Map<String,Object>> getRoutesRole(@RequestParam String roleId,  HttpServletRequest request)  {
		return rs.getRoutesRole(roleId);
    }

	@GetMapping("/role/routes/checked")
    public  List<Map<String,Object>> getRoutesRoleChecked(@RequestParam String roleId,  HttpServletRequest request)  {
		return rs.getRoutesRoleChecked(roleId);
    }
	
 
	
	@RequestMapping(value = "/role/addEndpoint", method = RequestMethod.POST)
	public Map<String,Object> addEndpoint(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.addEndpoint(model);
	}
	
	
	@RequestMapping(value= "/role/removeEndpoint", method = RequestMethod.DELETE)
	public void removeEndpoint(@RequestParam String roleId,@RequestParam String endpointId , HttpServletRequest request) throws SQLException {
		 
	  rs.removeEndpoint(roleId, endpointId);
	}
	
	@RequestMapping(value = "/role/saveRoutes", method = RequestMethod.POST)
	public List< Map<String,Object>> saveRoutes(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		
		String roleId= model.get("roleId").toString();
		String routes = model.get("routes").toString();
		rs.saveRoutes(roleId, routes);
		
		return rs.getRoutesRoleChecked(roleId);
		
	}
	
	
	

}
