package com.co.lowcode.backend.controller;


import java.util.List;
import java.util.Map;


import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.backend.model.RequestQuery;
import com.co.lowcode.backend.util.Util;
import com.co.lowcode.report.service.QueryService;

@RestController
public class BackendController {

	@Autowired
	QueryService qs;
	
	
	
	@Bean
	public QueryService jwtConfig() {
		return new QueryService();
	}

	@RequestMapping(value = "/query", method = RequestMethod.POST)
	public Object query(@RequestBody RequestQuery requestQuery, HttpServletRequest request) throws Exception {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		requestQuery.setUsername(username);
		return qs.query(requestQuery, 0, request.getHeader("authorization"));
	}

	@RequestMapping(value = "/service", method = RequestMethod.POST)
	public Object service(@RequestBody RequestQuery requestQuery, HttpServletRequest request) throws Exception {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		requestQuery.setUsername(username);
		List<Map<String, Object>> result = (List<Map<String, Object>>) qs.buildQuery(requestQuery, 0, request.getHeader("authorization")).get("QUERY");
		
	
		//return qs.createTempTable(result);
		 return result;
	}
	
	
	
	//Devuelve un objeto si el arreglo tiene una posición
		@RequestMapping(value = "/serviceFit", method = RequestMethod.POST)
		public Object serviceFit(@RequestBody RequestQuery requestQuery, HttpServletRequest request) throws Exception {
			String username = Util.getUserName(request.getHeader("authorization").substring(7));
			requestQuery.setUsername(username);
			String token = request.getHeader("authorization");
			
			 List<Map<String, Object>> result =  (List<Map<String, Object>>)qs.buildQuery(requestQuery, 0,token).get("QUERY");
			
			 if(result.size()==1) {
				return result.get(0);
			 }
			return result;
		}
		
		//Servicio publico 
		@RequestMapping(value = "/public/service", method = RequestMethod.POST)
		public Object publicService(@RequestBody RequestQuery requestQuery, @RequestParam(defaultValue="true") Boolean isArray) throws Exception {
			String token = "FIX";
			List<Map<String, Object>> result =  (List<Map<String, Object>>)qs.buildQuery(requestQuery, 1, token).get("QUERY");
			
			
			
			if(isArray) {
				return result;
			}
			 if(result.size()==1) {
				return result.get(0);
			 }
			return result;
		}
	
	
	
	@RequestMapping(value = "/insert", method = RequestMethod.POST)
	public void insert(@RequestBody RequestQuery requestQuery, @RequestParam String tableName, HttpServletRequest request) throws JSONException {
		 
		
		qs.insert(requestQuery, tableName);
	}
	
	
}
