package com.co.lowcode.sso.service;


import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
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
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.co.lowcode.sso.config.GeneralConfig;
import com.co.lowcode.sso.exception.ReportDuplicateException;
import com.co.lowcode.sso.model.RequestQuery;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;


@Service
public class ReportService {

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
	
	@Autowired
	private RedisService redisService;

	@Bean
	public RedisService redisServiceConfig() {
		return new RedisService();
	}
	
	@Transactional
	public Map<String, Object> createReport(Map<String, Object> model) throws TemplateNotFoundException,
		
		MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		if (!existReport(model)) {
			 
			RequestQuery rq = new RequestQuery();
			 
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("DESCRIPTION", model.get("DESCRIPTION"));
			params.put("NAME", model.get("NAME"));
			params.put("QUERY", model.get("QUERY"));
			params.put("TYPE", model.get("TYPE"));
			//params.put("UUID",   UUID.randomUUID() );
			params.put("PUBLIC_END",  model.get("PUBLIC_END") );
			params.put("CAPTCHA",  model.get("CAPTCHA") );

			rq.setParams(params);
						
			Map<String, Object> createdRow = queryService.create(params, "REPORT", "ID_REPORT");
			
			return createdRow;
			
		} else {
			throw new ReportDuplicateException(model.get("NAME").toString());
		}
		
	}
	
	@Transactional
	public Map<String, Object> updateReport(Map<String, Object> model) throws TemplateNotFoundException,
	
		MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		if (!existReport(model)) {
			 
			RequestQuery rq = new RequestQuery();
			 
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("DESCRIPTION", model.get("DESCRIPTION"));
			params.put("NAME", model.get("NAME"));
			params.put("QUERY", model.get("QUERY"));
			params.put("TYPE", model.get("TYPE"));
			
			//params.put("PUBLIC_END",  model.get("PUBLIC_END") );
			//params.put("CAPTCHA",  model.get("CAPTCHA") );
			 
			rq.setParams(params);
			
			Map<String, Object> where = new LinkedHashMap<String, Object>(); 
			where.put("ID_REPORT", model.get("ID_REPORT"));
			rq.setWhere(where);
			
						
			Map<String, Object> updatedRow = queryService.update(rq, "REPORT");
			
			redisService.deleteValueStartWith(model.get("UUID").toString());
			
			return updatedRow;
			
		} else {
			throw new ReportDuplicateException(model.get("NAME").toString());
		}
		
	}

	
	public List<Map<String, Object>> getReports(){
		String query = "SELECT ID_REPORT \"ID_REPORT\", CAPTCHA \"CAPTCHA\",DESCRIPTION \"DESCRIPTION\","
				+ " NAME \"NAME\", PUBLIC_END \"PUBLIC_END\", QUERY \"QUERY\", TYPE \"TYPE\", UUID \"UUID\""
				+ " FROM REPORT ORDER BY NAME,DESCRIPTION";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	
		
	
	public Boolean existReport(Map<String,Object> model) {
		String query = "SELECT 1  FROM REPORT WHERE NAME =:name";
		
		if(model.get("ID_REPORT")!=null && model.get("ID_REPORT")!="0" ) {
			query+= " AND ID_REPORT !=:reportId ";
		}
		
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("name", model.get("NAME"));
		
		if(model.get("ID_REPORT")!=null && model.get("ID_REPORT")!="0" ) {
			q.setParameter("reportId", model.get("ID_REPORT"));
		}
		
		 
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}
	
	


	public List<Map<String, Object>> getRolesReport(String reportId) {
		String query = "SELECT REPORT_ID \"REPORT_ID\", RP.NAME \"REPORT_NAME\", ROLE_ID \"ROLE_ID\", RL.NAME \"ROLE_NAME\"   "
				+ " FROM ROLE RL INNER JOIN  ROLE_REPORT  ON ( ROLE_ID= ID_ROLE )"
				+ " INNER JOIN REPORT RP ON ( ID_REPORT=REPORT_ID ) "
				+ " WHERE REPORT_ID =:reportId   "
				+ " ORDER BY RL.NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("reportId", reportId);

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	
	public List<Map<String, Object>> getReport(BigInteger reportId) {
		String query = "SELECT UUID FROM REPORT "
				+ " WHERE ID_REPORT =:reportId";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("reportId", reportId);

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

	
	public List<Map<String, Object>> getRolesReportChecked(String reportId) {
		String query = "SELECT :reportId \"REPORT_ID\", (CASE WHEN EM.ROLE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_ROLE \"ROLE_ID\", NAME \"ROLE_NAME\"   "
				+ " , (SELECT NAME FROM REPORT WHERE ID_REPORT= :reportId ) \"REPORT_NAME\" "
				+ " FROM ROLE LEFT JOIN (SELECT ROLE_ID, REPORT_ID FROM ROLE_REPORT WHERE REPORT_ID =:reportId ) EM  ON ( ROLE_ID= ID_ROLE )  "
				+ " ORDER BY NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("reportId", new BigInteger(reportId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

	
	public Boolean existRoleReport(Map<String, Object> model) {
		String query = "SELECT 1  FROM ROLE_REPORT WHERE REPORT_ID =:reportId AND ROLE_ID =:roleId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("reportId", model.get("REPORT_ID"));
		q.setParameter("roleId", model.get("ROLE_ID"));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}
	
		
	public Map<String, Object> addRole(Map<String, Object> model) throws SQLException {

		if (!existRoleReport(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("ROLE_ID", model.get("ROLE_ID"));
			params.put("REPORT_ID", model.get("REPORT_ID"));
			rq.setParams(params);

			Map<String, Object> createdRow = queryService.create(params, "ROLE_REPORT", "ROLE_ID");
			System.out.println("Insert role report" + params);
			return createdRow;
		}
		return null;

	}


	@Transactional
	public void removeRole(String reportId, String roleId) throws SQLException {

		String query = "DELETE FROM ROLE_REPORT WHERE ROLE_ID = :roleId AND REPORT_ID =:reportId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));
		q.setParameter("reportId", new BigInteger(reportId));
		q.executeUpdate();

	}

	  
}
	
	
	


