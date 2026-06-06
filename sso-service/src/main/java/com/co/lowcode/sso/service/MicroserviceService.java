package com.co.lowcode.sso.service;

 
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.co.lowcode.sso.config.GeneralConfig;
import com.co.lowcode.sso.exception.MicroserviceDuplicateException;
import com.co.lowcode.sso.model.RequestQuery;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;


@Service
public class MicroserviceService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private QueryService queryService;
	
	@Autowired
	private EndpointService endpointService;

	@Autowired
	RabbitMQSender rabbitMQSender;
	
	@Autowired
	private RoleService roleService;

	@Autowired
	private GeneralConfig config;

	@Autowired
	private Configuration configuration;
	
	

	public Map<String, Object> createInitMicroservice(Map<String, Object> model) throws TemplateNotFoundException,
																						MalformedTemplateNameException,
																						ParseException, IOException, 
																						TemplateException, MessagingException, 
																						SQLException  {
		
		
		Map<String, Object> endpointMicroservice = new LinkedHashMap<>();
		Map<String, Object> response = createMicroservice(model);
		Map<String, Object> role = new LinkedHashMap<>();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("METHOD", "POST");
		params.put("NUMBERPARAMS", 0);
		params.put("PATH", "/createTable");
		params.put("DESCRIPTION", "Create Table " + model.get("DESCRIPTION"));
		
		Map<String, Object> endpointCT = endpointService.createEndpoint(params);
		
		endpointMicroservice.put("ID_ENDPOINT", endpointCT.get("ID_ENDPOINT"));
		endpointMicroservice.put("ID_MICROSERVICE", response.get("ID_MICROSERVICE"));
		addEndpoint(endpointMicroservice);
		role.put("ID_ENDPOINT", endpointCT.get("ID_ENDPOINT"));
		role.put("ID_ROLE", 1);
		roleService.addEndpoint(role);
		
		params.put("DESCRIPTION", "Dynamic Query " + model.get("DESCRIPTION"));
		params.put("PATH", "/service");
		endpointCT = endpointService.createEndpoint(params);
		endpointMicroservice.put("ID_ENDPOINT", endpointCT.get("ID_ENDPOINT"));
		addEndpoint(endpointMicroservice);
		role.put("ID_ENDPOINT", endpointCT.get("ID_ENDPOINT"));
		roleService.addEndpoint(role);
		
		params.put("DESCRIPTION", "Dynamic Query " + model.get("DESCRIPTION"));
		params.put("PATH", "/serviceFit");
		endpointCT = endpointService.createEndpoint(params);
		endpointMicroservice.put("ID_ENDPOINT", endpointCT.get("ID_ENDPOINT"));
		addEndpoint(endpointMicroservice);
		role.put("ID_ENDPOINT", endpointCT.get("ID_ENDPOINT"));
		roleService.addEndpoint(role);
		
		
		params.put("DESCRIPTION", "Get Metadata " + model.get("DESCRIPTION"));
		params.put("PATH", "/getMetadata");
		params.put("METHOD", "GET");
		params.put("NUMBERPARAMS", 1);
		endpointCT = endpointService.createEndpoint(params);
		endpointMicroservice.put("ID_ENDPOINT", endpointCT.get("ID_ENDPOINT"));
		addEndpoint(endpointMicroservice);
		role.put("ID_ENDPOINT", endpointCT.get("ID_ENDPOINT"));
		roleService.addEndpoint(role);
		
		

		return response;
	}
	
	@Transactional
	public Map<String, Object> createMicroservice(Map<String, Object> model) throws TemplateNotFoundException,
		
		MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		if (!existMicroservice(model)) {
			 
			RequestQuery rq = new RequestQuery();
			 
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("DESCRIPTION", model.get("DESCRIPTION"));
			params.put("REQUESTURI", model.get("REQUESTURI"));
			params.put("SERVICEID", model.get("SERVICEID"));
			params.put("TARGETURIPATH", model.get("TARGETURIPATH"));
			params.put("TARGETURLHOST", model.get("TARGETURLHOST"));
			params.put("TARGETURLPORT",  model.get("TARGETURLPORT") );
			 
			rq.setParams(params);
						
			Map<String, Object> createdMs = queryService.create(params, "MICROSERVICE", "ID_MICROSERVICE");
			
			return createdMs;
			
		} else {
			throw new MicroserviceDuplicateException(model.get("DESCRIPTION").toString());
		}
		
	}
	
	@Transactional
	public Map<String, Object> updateMicroservice(Map<String, Object> model) throws TemplateNotFoundException,
	
		MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		if (!existMicroservice(model)) {
			 
			RequestQuery rq = new RequestQuery();
			 
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("DESCRIPTION", model.get("DESCRIPTION"));
			params.put("REQUESTURI", model.get("REQUESTURI"));
			params.put("SERVICEID", model.get("SERVICEID"));
			params.put("TARGETURIPATH", model.get("TARGETURIPATH"));
			params.put("TARGETURLHOST", model.get("TARGETURLHOST"));
			params.put("TARGETURLPORT",  model.get("TARGETURLPORT") );
			 
			rq.setParams(params);
			
			Map<String, Object> where = new LinkedHashMap<String, Object>(); 
			where.put("ID_MICROSERVICE", model.get("ID_MICROSERVICE"));
			rq.setWhere(where);
						
						
			Map<String, Object> updatedMs = queryService.update(rq, "MICROSERVICE");
			
			return updatedMs;
			
		} else {
			throw new MicroserviceDuplicateException(model.get("DESCRIPTION").toString());
		}
		
	}

	
	public List<Map<String, Object>> getMicroservices(){
		String query = "SELECT ID_MICROSERVICE AS \"ID_MICROSERVICE\", CREATEDDATE \"CREATEDDATE\","
				+ " DESCRIPTION \"DESCRIPTION\", REQUESTURI \"REQUESTURI\", SERVICEID \"SERVICEID\","
				+ " TARGETURIPATH \"TARGETURIPATH\", TARGETURLHOST \"TARGETURLHOST\",TARGETURLPORT \"TARGETURLPORT\""
				+ " FROM MICROSERVICE ORDER BY DESCRIPTION";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	public List<Map<String, Object>> getMicroservice(String serviceId){
		String query = "SELECT * FROM MICROSERVICE WHERE SERVICEID =:serviceId";
		RequestQuery requestQuery = new RequestQuery();
		Map<String,Object> params = new LinkedHashMap<>();
		params.put("serviceId", serviceId);
		requestQuery.setParams(params);
		return queryService.getResultQuery(query, requestQuery);
	}
	
	
	
	public List<Map<String, Object>> getEndpointsMicroservice(String msId){
		String query = "SELECT ID_MICROSERVICE \"ID_MICROSERVICE\", DESCRIPTION \"DESCRIPTION\" ,"
				+ " ID_ENDPOINT \"ID_ENDPOINT\", METHOD \"METHOD\", NUMBERPARAMS \"NUMBERPARAMS\",\"PATH\" "
				+ " FROM ENDPOINT INNER JOIN  ENDPOINT_MICROSERVICE ON ( ENDPOINT_ID=ID_ENDPOINT ) "
				+ " INNER JOIN MICROSERVICE ON ( MICROSERVICE_ID=ID_MICROSERVICE ) "
				+ " WHERE MICROSERVICE_ID =:msId  "
				+ " ORDER BY METHOD, PATH";
		
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("msId", new BigInteger(msId));
		
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		 	
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();
		
	}
	

	public List<Map<String, Object>> getEndpointsMicroserviceChecked(String msId){
		
		String query = "SELECT :msId \"ID_MICROSERVICE\", (CASE WHEN EM.MICROSERVICE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\", "
				+ "ID_ENDPOINT \"ID_ENDPOINT\", METHOD \"METHOD\",NUMBERPARAMS \"NUMBERPARAMS\", PATH \"PATH\""
				+ " , (SELECT DESCRIPTION FROM MICROSERVICE WHERE ID_MICROSERVICE= :msId ) MICROSERVICE "
				+ " FROM ENDPOINT"
				+ " LEFT JOIN (SELECT MICROSERVICE_ID, ENDPOINT_ID FROM ENDPOINT_MICROSERVICE WHERE MICROSERVICE_ID =:msId ) EM  "
				+ " ON ( ENDPOINT_ID=ID_ENDPOINT )  "
				+ " ORDER BY METHOD, PATH";
		
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("msId", new BigInteger(msId));
		
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		 	
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();
		
	}
	

	public List<Map<String, Object>> getAppsMicroservice(String msId){
		String query = "SELECT ID_MICROSERVICE \"MICROSERVICE_ID\", DESCRIPTION \"MICROSERVICE_NAME\", APP_ID \"APP_ID\", NAME \"APP_NAME\" "
				+ " FROM APP INNER JOIN  APP_MICROSERVICE ON ( APP_ID=ID_APP ) "
				+ " INNER JOIN MICROSERVICE ON ( MICROSERVICE_ID=ID_MICROSERVICE ) "
				+ " WHERE MICROSERVICE_ID =:msId  "
				+ " ORDER BY NAME ";
		
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("msId", new BigInteger(msId));
		
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		 	
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();
		
	}

	
	public List<Map<String, Object>> getAppsMicroserviceChecked(String msId){
		
		String query = "SELECT :msId \"MICROSERVICE_ID\", (CASE WHEN EM.MICROSERVICE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_APP \"APP_ID\", NAME \"APP_NAME\"  "
				+ " , (SELECT DESCRIPTION FROM MICROSERVICE WHERE ID_MICROSERVICE= :msId ) \"MICROSERVICE_NAME\" "
				+ " FROM APP LEFT JOIN (SELECT MICROSERVICE_ID, APP_ID FROM APP_MICROSERVICE WHERE MICROSERVICE_ID =:msId ) EM  ON ( APP_ID=ID_APP )  "
				+ " ORDER BY NAME ";
		
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("msId", new BigInteger(msId));
		
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		 	
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();
		
	}
	
				
		
	public Boolean existEndpointMicroservice(Map<String,Object> model) {
			String query = "SELECT 1  FROM ENDPOINT_MICROSERVICE WHERE ENDPOINT_ID = :endpointId AND MICROSERVICE_ID =:msId";
			Query q = entityManager.createNativeQuery(query);
			q.setParameter("endpointId", model.get("ID_ENDPOINT"));
			q.setParameter("msId", model.get("ID_MICROSERVICE"));
			
			org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
			hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			List<Map<String, Object>> res = hibernateQuery.list();
			if (res.size() > 0) {
				return true;
			}
			return false;
	}
	
	public Boolean existMicroservice(Map<String,Object> model) {
		String query = "SELECT 1  FROM MICROSERVICE WHERE DESCRIPTION =:descripcion";
		
		if(model.get("ID_MICROSERVICE")!=null && model.get("ID_MICROSERVICE")!="0" ) {
			query+= " AND ID_MICROSERVICE !=:msId ";
		}
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("descripcion", model.get("DESCRIPTION"));
		
		if(model.get("ID_MICROSERVICE")!=null && model.get("ID_MICROSERVICE")!="0" ) {
			q.setParameter("msId", model.get("ID_MICROSERVICE"));
		}
		
		 
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}
	
	
	@Transactional
	public Map<String, Object> addEndpoint( Map<String, Object> model ) throws SQLException {
		 
		if (!existEndpointMicroservice(model)) {
			
			RequestQuery rq = new RequestQuery();
			 
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("ENDPOINT_ID", model.get("ID_ENDPOINT"));
			params.put("MICROSERVICE_ID", model.get("ID_MICROSERVICE"));
			rq.setParams(params);
						
			Map<String, Object> createdEndpointMS = queryService.create(params, "ENDPOINT_MICROSERVICE", "ENDPOINT_ID");
			 
			return createdEndpointMS;
		}
		return null;
	}
	
	
	@Transactional	
	public void removeEndpoint( String msId,  String endpointId ) throws SQLException {
			String query = "DELETE FROM ENDPOINT_MICROSERVICE WHERE ENDPOINT_ID = :endpointId AND MICROSERVICE_ID =:msId";
			Query q = entityManager.createNativeQuery(query);
			q.setParameter("endpointId", new BigInteger(endpointId));
			q.setParameter("msId", new BigInteger(msId));
			q.executeUpdate();
	}	 
}