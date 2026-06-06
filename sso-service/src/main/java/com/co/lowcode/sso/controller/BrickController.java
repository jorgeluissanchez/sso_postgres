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

import com.co.lowcode.sso.service.BrickService;
import com.co.lowcode.sso.service.GroupService;
import com.co.lowcode.sso.util.Util;

@RestController
@RequestMapping(value = { "/brick" }, produces = { "application/json" })
public class BrickController {

	@Autowired
	BrickService brickService;

	@RequestMapping(method = RequestMethod.GET)
	public List<Map<String, Object>> getBricks(HttpServletRequest request) throws SQLException {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return brickService.getBricks();
	}
}