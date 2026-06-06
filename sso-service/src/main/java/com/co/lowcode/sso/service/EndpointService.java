package com.co.lowcode.sso.service;


import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.stereotype.Service;


import com.co.lowcode.sso.config.GeneralConfig;

import com.co.lowcode.sso.exception.EndpointDuplicateException;
 
 
import com.co.lowcode.sso.model.RequestQuery;


import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;

import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;


@Service
public class EndpointService {
	
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private QueryService queryService;

	@Autowired
	RabbitMQSender rabbitMQSender;

	@Autowired
	private GeneralConfig config;

	@Autowired
	private Configuration configuration;
	
	@Transactional
	public Map<String, Object> createEndpoint(Map<String, Object> model) throws TemplateNotFoundException,
		
		MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		if (!existEndpoint(model)) {
			 
			RequestQuery rq = new RequestQuery();
			 
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("METHOD", model.get("METHOD"));
			params.put("NUMBERPARAMS", model.get("NUMBERPARAMS") != null ? Integer.parseInt(model.get("NUMBERPARAMS").toString()):0);
			params.put("PATH", model.get("PATH"));
			params.put("DESCRIPTION",model.get("DESCRIPTION"));
			 
			rq.setParams(params);
						
			Map<String, Object> createdMs = queryService.create(params, "ENDPOINT", "ID_ENDPOINT");
			
			return createdMs;
			
		} else {
			throw new EndpointDuplicateException(model.get("PATH").toString());
		}
		
	}
	
	@Transactional
	public Map<String, Object> updateEndpoint(Map<String, Object> model) throws TemplateNotFoundException,
	
		MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		if (!existEndpoint(model)) {
			 
			RequestQuery rq = new RequestQuery();
			 
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("METHOD", model.get("METHOD"));
			params.put("NUMBERPARAMS", Integer.parseInt(model.get("NUMBERPARAMS").toString()));
			params.put("PATH", model.get("PATH"));
			params.put("DESCRIPTION",model.get("DESCRIPTION"));
			 
			rq.setParams(params);
			
			Map<String, Object> where = new LinkedHashMap<String, Object>(); 
			where.put("ID_ENDPOINT", model.get("ID_ENDPOINT"));
			rq.setWhere(where);
						
			
						
			Map<String, Object> updatedMs = queryService.update(rq, "ENDPOINT");
			
			return updatedMs;
			
		} else {
			throw new EndpointDuplicateException(model.get("PATH").toString());
		}
		
	}

	
	public List<Map<String, Object>> getEndpoints(){
		String query = "SELECT  ID_ENDPOINT \"ID_ENDPOINT\", METHOD \"METHOD\","
				+ " NUMBERPARAMS \"NUMBERPARAMS\", PATH \"PATH\", DESCRIPTION \"DESCRIPTION\""
				+ " FROM ENDPOINT ORDER BY PATH,METHOD ";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	
	public Boolean existEndpoint(Map<String,Object> model) {
		String query = "SELECT 1 FROM ENDPOINT WHERE PATH =:path AND METHOD =:method AND DESCRIPTION =:description ";
		
		if(model.get("ID_ENDPOINT")!=null && model.get("ID_ENDPOINT")!="0" ) {
			query+= " AND ID_ENDPOINT !=:endpointId ";
		}
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("method", model.get("METHOD"));
		q.setParameter("path", model.get("PATH"));
		q.setParameter("description", model.get("DESCRIPTION"));
		
		
		if(model.get("ID_ENDPOINT")!=null && model.get("ID_ENDPOINT")!="0" ) {
			q.setParameter("endpointId", model.get("ID_ENDPOINT"));
		}
		
		 
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}
	
	
	public List<Map<String, Object>> getMicroservicesEndpoint(String endpointId){
		String query = "SELECT  ID_ENDPOINT \"ID_ENDPOINT\", \"METHOD\", NUMBERPARAMS \"NUMBERPARAMS\", "
				+ " \"PATH\"  ID_MICROSERVICE \"ID_MICROSERVICE\", DESCRIPTION \"MICROSERVICE\", REQUESTURI \"REQUESTURI\", SERVICEID \"SERVICEID\" "
				+ " FROM MICROSERVICE INNER JOIN ENDPOINT_MICROSERVICE ON ( MICROSERVICE_ID=ID_MICROSERVICE ) "
				+ " INNER JOIN ENDPOINT ON ( ENDPOINT_ID=ID_ENDPOINT ) "
				+ " WHERE ENDPOINT_ID =:endpointId   "
				+ " ORDER BY DESCRIPTION ";
		
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("endpointId", endpointId);
		
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		 	
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();
		
	}
	
	
	public List<Map<String, Object>> getMicroservicesEndpointChecked(String endpointId){
		String query = "SELECT :endpointId \"ID_ENDPOINT\", (CASE WHEN EM.ENDPOINT_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\", "
				+ " ID_MICROSERVICE \"ID_MICROSERVICE\", DESCRIPTION \"MICROSERVICE\", REQUESTURI \"REQUESTURI\", SERVICEID \"SERVICEID\" "
				+ " FROM MICROSERVICE LEFT JOIN (SELECT MICROSERVICE_ID, ENDPOINT_ID FROM ENDPOINT_MICROSERVICE WHERE ENDPOINT_ID =:endpointId ) EM  ON ( MICROSERVICE_ID=ID_MICROSERVICE )  "
				+ " ORDER BY DESCRIPTION ";
		
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("endpointId",  new BigInteger(endpointId));
		
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		 	
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();
		
	}
	
	
	

	public List<Map<String, Object>> getRolesEndpoint(String endpointId) {
		String query = "SELECT   ID_ENDPOINT \"ID_ENDPOINT\", \"METHOD\", NUMBERPARAMS \"NUMBERPARAMS\","
				+ " \"PATH\" , ID_ROLE \"ID_ROLE\", NAME \"ROLE\"   "
				+ " FROM ROLE INNER JOIN  ROLE_ENDPOINT  ON ( ROLE_ID= ID_ROLE )"
				+ " INNER JOIN ENDPOINT ON ( ID_ENDPOINT=ENDPOINT_ID ) "
				+ " WHERE ENDPOINT_ID =:endpointId   "
				+ " ORDER BY NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("endpointId", new BigInteger(endpointId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

	
	public List<Map<String, Object>> getRolesEndpointChecked(String endpointId) {
		String query = "SELECT :endpointId \"ID_ENDPOINT\", (CASE WHEN EM.ROLE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_ROLE \"ID_ROLE\", NAME \"ROLE\" "
				+ " FROM ROLE LEFT JOIN (SELECT ROLE_ID, ENDPOINT_ID FROM ROLE_ENDPOINT WHERE ENDPOINT_ID =:endpointId ) EM  ON ( ROLE_ID= ID_ROLE )  "
				+ " ORDER BY NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("endpointId", new BigInteger(endpointId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

		

}
