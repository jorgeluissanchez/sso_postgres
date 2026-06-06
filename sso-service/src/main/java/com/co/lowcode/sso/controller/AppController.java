package com.co.lowcode.sso.controller;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.sso.model.Component;
import com.co.lowcode.sso.rethinkdb.RethinkDBConnectionFactory;
import com.co.lowcode.sso.service.AppService;
import com.co.lowcode.sso.util.Util;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;

@RestController

@RequestMapping(value = { "/app" }, produces = { "application/json" })
public class AppController {

    protected final Logger log = LoggerFactory.getLogger(AppController.class);
    private static final RethinkDB r = RethinkDB.r;
    
    @Autowired
    AppService as;
    
    @Autowired
    private RethinkDBConnectionFactory connectionFactory;

   
   
    @RequestMapping(method = RequestMethod.POST)
    public Map<String, Object> post(@RequestBody Map<String,Object> app, HttpServletRequest request) throws  SQLException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
    	return as.save(app, username);
    }
   
    @GetMapping("/getApp")
    public  Object getApp(@RequestParam String uuid, @RequestParam String type ) throws  ConnectException {
    	
    	switch (type) {
    		case "structure":
    			return as.getStructure(uuid);
    		case "apiDefinition":
    			return as.getApiDefinition(uuid);
    		case "flowDefinition":
    			return as.getFlowDefinition(uuid);
    		case "dataModel":
    			return as.getDataModel(uuid);
    		case "style":
    			return as.getStyle(uuid);
    	}
    	return null;
    }
    
    @GetMapping("/getApiDefinition")
    public  Object getApiDefinition(@RequestParam String uuid) throws  ConnectException {
        return as.getApiDefinition(uuid);
    }
    
    @GetMapping("/getFlowDefinition")
    public  Object getFlowDefinition(@RequestParam String uuid) throws  ConnectException {
        return as.getFlowDefinition(uuid);
    }
    
    @RequestMapping(value="/getDataModel", method = RequestMethod.GET)
    public  Object getDataModel(@RequestParam String uuid) throws  ConnectException {
        return as.getDataModel(uuid);
        
    } 
    
    @GetMapping("/getStyle")
    public  Object getStyle(@RequestParam String uuid) throws  ConnectException {
        return as.getStyle(uuid);
    }
    
}
