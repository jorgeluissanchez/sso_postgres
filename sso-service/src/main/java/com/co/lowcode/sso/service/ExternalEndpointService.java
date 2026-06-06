package com.co.lowcode.sso.service;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.HttpMethod;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ExternalEndpointService {
	
	@Autowired
	QueryService qs;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	@Autowired
	RestTemplate restTemplate;
	
	public Object getExternalEndpoint(Map<String,Object> body, String username) {
		String query = "SELECT EE.* FROM EXTERNAL_ENDPOINT EE "
				+ "INNER JOIN ROLE_EXTERNAL_ENDPOINT REE ON EE.ID_EXTERNAL_ENDPOINT = REE.EXTERNAL_ENDPOINT_ID "
				+ "INNER JOIN ROLE R ON R.ID_ROLE = REE.ROLE_ID "
				+ "INNER JOIN ROLE_USERS RU ON RU.ROLE_ID = REE.ROLE_ID "
				+ "INNER JOIN USERS U ON U.id_user = RU.user_id "
				+ "WHERE U.USERNAME = :USERNAME AND EE.UUID = :UUID";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("USERNAME", username);
		q.setParameter("UUID", body.get("uuid"));
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		
		Map<String, Object> externalEndpoint = res.get(0);
		Map<String,Object> params = (Map<String, Object>) body.get("params");
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		String requestBody = "";
		
		if(body.get("body") != null) {
			requestBody = body.get("body").toString();
		}
		
		
		HttpEntity<String> request = 
			      new HttpEntity<String>(requestBody, headers);
		
		if(externalEndpoint.get("method").equals("GET")) {
			try {
				Object s = restTemplate.getForObject(externalEndpoint.get("url").toString(), Object.class, params);
				return s;
			}catch(Exception e) {
				
			}
			
		}else if(externalEndpoint.get("method").equals("POST")) {
			Object s = restTemplate.postForObject(externalEndpoint.get("url").toString(), request, Object.class, params);
			return s;
		}
	

		return null;
	}

}
