package com.co.lowcode.sso.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.co.lowcode.sso.model.RequestQuery;

@Service
public class ScreenService {

	protected final Logger log = LoggerFactory.getLogger(ScreenService.class);

	@Autowired
	QueryService qs;
	
	@Autowired
	AppService appService;
	

	@Transactional
	public Map<String, Object> save(Map<String, Object> screen, Integer idApp, String username) throws SQLException {
		if(appService.grantedApp(username, idApp)) {
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("NAME", screen.get("NAME"));
		//	params.put("CREATEDAT", new Date());
			
			Map<String, Object> idScreen = qs.create(params, "SCREEN", "ID_SCREEN");
			
			RequestQuery rq = new RequestQuery();
			params = new LinkedHashMap<>();
			params.put("APP_ID", idApp);
			params.put("SCREEN_ID", idScreen.get("ID_SCREEN"));
			rq.setParams(params);
			qs.insert(rq, "APP_SCREEN");
			
			return idScreen;
		} else {
			throw new RuntimeException("Usuario no tiene permiso para esta acción");
		}
		
	}

}
