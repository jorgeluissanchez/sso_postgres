package com.co.lowcode.sso.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.sso.service.GroupService;
import com.co.lowcode.sso.util.Util;

@RestController
@RequestMapping(value = { "/group" }, produces = { "application/json" })
public class GroupController {

	@Autowired
	GroupService groupService;

	@RequestMapping(method = RequestMethod.POST)
	public Map<String, Object> save(@RequestBody Map<String, Object> group, HttpServletRequest request) throws SQLException {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return groupService.save(group,username);
		
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public List<Map<String, Object>> getGroupsByEditor(HttpServletRequest request) throws SQLException {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return groupService.getGroupsByEditor(username);
	}
	
	@RequestMapping(value= "bindUserGroup", method = RequestMethod.POST)
	public void bindUserGroup(@RequestBody Map<String, Object> group, HttpServletRequest request) throws SQLException {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		groupService.bindUserGroup(group,username);
	}
	
	@RequestMapping(value= "bindAppGroup", method = RequestMethod.POST)
	public void bindAppGroup(@RequestBody Map<String, Object> group, HttpServletRequest request) throws SQLException {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		groupService.bindAppGroup(group, username);
	}
}