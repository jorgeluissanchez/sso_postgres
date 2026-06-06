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
import com.co.lowcode.sso.service.UserService;
import com.co.lowcode.sso.util.Util;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@RestController
public class UserController {

	@Autowired
	UserService us;
	
	@Autowired
	RoleService roleService;

	@RequestMapping(value = "/createAccount", method = RequestMethod.POST)
	public Map<String,Object> createAccount(@RequestBody Map<String, Object> user, @RequestParam(defaultValue="")  String pathTemplate) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return us.createAccount(user, "", pathTemplate);
	}
	

	@RequestMapping(value = "/updateAccount", method = RequestMethod.PUT)
	public Map<String,Object> updateAccount(@RequestBody Map<String, Object> user) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return us.updateAccount(user, "");
	}
	
	@RequestMapping(value = "/activateAccount", method = RequestMethod.GET)
	public void activateAccount(@RequestParam String token, @RequestParam String password) {
		us.activateAccount(token, password);
	}
	
	@RequestMapping(value="/forgotPassword",method={RequestMethod.GET}, produces={"application/json"})
	public void forgotPassword(@RequestParam(value="email") String email) throws TemplateNotFoundException, 
	MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException{
		us.forgotPassword(email);	
	}
	
	@GetMapping("/getUsers")
    public  List<Map<String,Object>> getUsers(  HttpServletRequest request)  {
		return us.getUsers();
    }
	
	@GetMapping("/getRoles")
    public  List<Map<String,Object>> getRoles(  HttpServletRequest request)  {
		return us.getRoles();
    }
    
	@GetMapping("/getRolesByUsername")
    public  List<Map<String,Object>> getRolesByUsername(HttpServletRequest request)  {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return us.getRoleByUsername(username);
    }
	
	
	@GetMapping("/user/roles")
    public  List<Map<String,Object>> getRolesUser(@RequestParam String userId,   HttpServletRequest request)  {
		return us.getRolesUser(userId);
    }
	
	
	@GetMapping("/user/roles/checked")
    public  List<Map<String,Object>> getRolesUserChecked(@RequestParam String userId,   HttpServletRequest request)  {
		return us.getRolesUserChecked(userId);
    }
	
	
	
	@RequestMapping(value = "/bindUserRole", method = RequestMethod.POST)
	public Map<String,Object> bindUserRole(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return roleService.addUser(model);
	}
	

	@RequestMapping(value = "/unbindUserRole", method = RequestMethod.DELETE)
	public  void unbindUserRole(@RequestParam String userId,@RequestParam String roleId  ) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		 roleService.removeRole(roleId, userId );
	}
	
    

}
