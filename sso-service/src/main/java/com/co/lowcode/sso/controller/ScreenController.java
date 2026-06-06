package com.co.lowcode.sso.controller;

import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.sso.service.ScreenService;
import com.co.lowcode.sso.util.Util;

@RestController
@RequestMapping(value = { "/screen" }, produces = { "application/json" })
public class ScreenController {
	
	@Autowired
    ScreenService screenService;
	
	@RequestMapping( method = RequestMethod.POST)
    public Map<String, Object> save(@RequestBody Map<String,Object> screen,
    		@RequestParam Integer idApp, HttpServletRequest request) throws SQLException {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return screenService.save(screen, idApp, username);
    }

}
