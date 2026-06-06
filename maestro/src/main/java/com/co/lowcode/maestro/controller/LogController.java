package com.co.lowcode.audit.controller;
 
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.audit.model.Log;
import com.co.lowcode.audit.service.LogService;
import com.co.lowcode.audit.util.Util;

@RestController
@RequestMapping( produces = { "application/json" })
public class LogController  {
	@Autowired
	private LogService logService;
	
	@RequestMapping(value = "log", method = RequestMethod.POST)
	public void log(@RequestBody Log log, HttpServletRequest request) throws Exception {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		log.setUsuario(username);
		log.setCreatedAt(new Date());
		logService.save(log);
	}
	
	@RequestMapping(value = "log", method = RequestMethod.GET)
	public List<Log> log(@RequestParam Date startDate, @RequestParam Date endDate, @RequestParam String entidad, HttpServletRequest request) throws Exception {
		return logService.getLog(startDate, endDate, entidad);
	}
}