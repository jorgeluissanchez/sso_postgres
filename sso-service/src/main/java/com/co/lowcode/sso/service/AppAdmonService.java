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
import org.springframework.stereotype.Service;

import com.co.lowcode.sso.config.GeneralConfig;
import com.co.lowcode.sso.exception.AppDuplicateException;
import com.co.lowcode.sso.model.RequestQuery;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@Service
public class AppAdmonService {

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
	public Map<String, Object> createApp(Map<String, Object> model) throws TemplateNotFoundException,

			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException,
			SQLException {
		if (!existApp(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("NAME", model.get("NAME"));
			params.put("DESCRIPTION", model.get("DESCRIPTION"));
			params.put("OWNER", model.get("OWNER"));
			params.put("ROOTSCREEN", model.get("ROOTSCREEN"));
			//params.put("CREATEDAT", new Date());
			//params.put("UPDATEAT", new Date());
			

			rq.setParams(params);

			Map<String, Object> createdRow = queryService.create(params, "APP", "ID_APP");

			return createdRow;

		} else {
			throw new AppDuplicateException(model.get("NAME").toString());
		}

	}

	@Transactional
	public Map<String, Object> updateApp(Map<String, Object> model) throws TemplateNotFoundException,

			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException,
			SQLException {
		if (!existApp(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("NAME", model.get("NAME"));
			params.put("DESCRIPTION", model.get("DESCRIPTION"));
			params.put("OWNER", model.get("OWNER"));
			params.put("ROOTSCREEN", model.get("ROOTSCREEN"));
			//params.put("UPDATEAT", new Date());

			rq.setParams(params);
			
			Map<String, Object> where = new LinkedHashMap<String, Object>(); 
			where.put("ID_APP", model.get("ID_APP"));
			rq.setWhere(where);

			Map<String, Object> updatedApp = queryService.update(rq, "APP");

			return updatedApp;

		} else {
			throw new AppDuplicateException(model.get("NAME").toString());
		}

	}

	public List<Map<String, Object>> getApplications() {
		String query = "SELECT ID_APP \"ID_APP\", NAME \"NAME\", DESCRIPTION \"DESCRIPTION\", "
				+ "OWNER \"OWNER\", ROOTSCREEN \"ROOTSCREEN\", CREATEDAT \"CREATEDAT\", UPDATEAT \"UPDATEAT\"  "
				+ "FROM APP ORDER BY NAME ";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}



	
	
	public List<Map<String, Object>> getRoutesApp(String appId) {

		String query = "SELECT APP_ID \"APP_ID\", ROUTE_ID \"ROUTE_ID\","
				+ " RT.NAME \"ROUTE_NAME\", APP.NAME \"APP_NAME\", COALESCE(IDPARENT,0) \"ROUTE_PARENT\", PATH \"ROUTE_PATH\" "
				+ " FROM ROUTE RT INNER JOIN APP_ROUTE  ON ( ROUTE_ID= ID_ROUTE ) "
				+ " INNER JOIN APP ON  ( APP_ID=ID_APP ) " + " WHERE APP_ID =:appId   " + " ORDER BY RT.NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId",new BigInteger(appId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

	public List<Map<String, Object>> getRoutesAppChecked(String appId) {

		String query = "SELECT :appId \"APP_ID\", (CASE WHEN EM.ROUTE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_ROUTE \"ROUTE_ID\", RT.NAME \"ROUTE_NAME\", COALESCE(IDPARENT,0) \"ROUTE_PARENT\" , PATH \"ROUTE_PATH\"  "
				+ " , (SELECT NAME FROM APP WHERE ID_APP= :appId ) \"APP_NAME\" "
				+ " FROM ROUTE RT LEFT JOIN (SELECT ROUTE_ID, APP_ID FROM APP_ROUTE WHERE APP_ID =:appId ) EM  ON ( ROUTE_ID= ID_ROUTE )  "
				+ " ORDER BY RT.NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(appId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	
	
	
	public List<Map<String, Object>> getRolesAppChecked(String appId) {
		String query = "SELECT :appId \"ID_APP\", (CASE WHEN EM.ROLE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_ROLE \"ID_ROLE\", NAME \"ROLE\"  "
				+ " FROM ROLE LEFT JOIN (SELECT ROLE_ID, APP_ID FROM ROLE_APP WHERE APP_ID =:appId ) EM  ON ( ROLE_ID= ID_ROLE )  "
				+ " ORDER BY NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(appId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}


	
	public List<Map<String, Object>> getMicroservicesApp(String appId) {

		String query = "SELECT  APP_ID \"APP_ID\", NAME \"APP_NAME\", MICROSERVICE_ID \"MICROSERVICE_ID\","
				+ " DESCRIPTION \"MICROSERVICE_NAME\" "
				+ " FROM MICROSERVICE INNER JOIN  APP_MICROSERVICE ON (MICROSERVICE_ID=ID_MICROSERVICE) "
				+ " INNER JOIN APP ON ( ID_APP=APP_ID ) "
				+ " WHERE APP_ID =:appId AND    "
				+ " ORDER BY DESCRIPTION ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(appId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

	
	public List<Map<String, Object>> getMicroservicesAppChecked(String appId) {

		String query = "SELECT :appId \"APP_ID\", (CASE WHEN EM.MICROSERVICE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_MICROSERVICE \"MICROSERVICE_ID\" , DESCRIPTION \"MICROSERVICE_NAME\"  "
				+ " , (SELECT NAME FROM APP WHERE ID_APP= :appId ) \"APP_NAME\" "
				+ " FROM MICROSERVICE LEFT JOIN (SELECT ID_MICROSERVICE MICROSERVICE_ID, APP_ID FROM APP_MICROSERVICE WHERE APP_ID =:appId ) EM  ON ( MICROSERVICE_ID=ID_MICROSERVICE )  "
				+ " ORDER BY DESCRIPTION  ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(appId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	

	public Boolean existRoleApp(Map<String, Object> model) {
		String query = "SELECT 1  FROM ROLE_APP WHERE ROLE_ID =:roleId AND APP_ID =:appId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", model.get("ID_ROLE"));
		q.setParameter("appId", model.get("ID_APP"));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}


	public Boolean existMicroserviceApp(Map<String, Object> model) {
		String query = "SELECT 1  FROM APP_MICROSERVICE WHERE ID_MICROSERVICE = :msId AND APP_ID =:appId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("msId", model.get("MICROSERVICE_ID"));
		q.setParameter("appId", model.get("APP_ID"));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}

	public Boolean existRouteApp(Map<String, Object> model) {
		String query = "SELECT 1  FROM APP_ROUTE WHERE APP_ID = :appId AND ROUTE_ID =:routeId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(model.get("APP_ID").toString()));
		q.setParameter("routeId", new BigInteger(model.get("ROUTE_ID").toString()));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}

	public Boolean existApp(Map<String, Object> model) {
		String query = "SELECT 1  FROM APP WHERE NAME = :name";

		if (model.get("ID_APP") != null && model.get("ID_APP") != "0") {
			query += " AND ID_APP !=:appId ";
		}

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("name", model.get("NAME"));

		if (model.get("ID_APP") != null && model.get("ID_APP") != "0") {
			q.setParameter("appId", model.get("ID_APP"));
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
	public Map<String, Object> addMicroservice(Map<String, Object> model) throws SQLException {

		if (!existMicroserviceApp(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("ID_MICROSERVICE", model.get("MICROSERVICE_ID"));
			params.put("APP_ID", model.get("APP_ID"));
			rq.setParams(params);

			Map<String, Object> createdRow = queryService.create(params, "APP_MICROSERVICE", "APP_ID");

			return createdRow;
		}
		return null;

	}
	
	
	@Transactional
	public Map<String, Object> addRole(Map<String, Object> model) throws SQLException {

		if (!existRoleApp(model)) {
			RequestQuery rq = new RequestQuery();
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("ROLE_ID", new BigInteger(model.get("ID_ROLE").toString()));
			params.put("APP_ID", new BigInteger(model.get("ID_APP").toString()));
			rq.setParams(params);
			Map<String, Object> createdRow = queryService.create(params, "ROLE_APP", "APP_ID");
			return createdRow;
		}
		return null;

	}


	@Transactional
	public Map<String, Object> addRoute(Map<String, Object> model) throws SQLException {

		if (!existRouteApp(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("APP_ID", model.get("APP_ID"));
			params.put("ROUTE_ID", model.get("ROUTE_ID"));
			rq.setParams(params);

			Map<String, Object> createdRow = queryService.create(params, "APP_ROUTE", "ROUTE_ID");

			return createdRow;
		}
		return null;

	}

	@Transactional
	public void removeMicroservice(String appId, String msId) throws SQLException {

		String query = "DELETE FROM APP_MICROSERVICE WHERE ID_MICROSERVICE= :msId AND APP_ID =:appId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("msId", new BigInteger(msId));
		q.setParameter("appId", new BigInteger(appId));
		q.executeUpdate();

	}
	
	@Transactional
	public void removeRole(String appId, String roleId) throws SQLException {

		String query = "DELETE FROM ROLE_APP WHERE ROLE_ID= :roleId AND APP_ID =:appId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));
		q.setParameter("appId", new BigInteger(appId));
		q.executeUpdate();

	}

	@Transactional
	public void removeRoute(String appId, String routeId) throws SQLException {

		String query = "DELETE FROM APP_ROUTE WHERE ROUTE_ID = :routeId AND APP_ID =:appId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(appId));
		q.setParameter("routeId", new BigInteger(routeId));
		q.executeUpdate();

	}

	@Transactional
	public void saveRoutes(String appId, String routes) throws SQLException {

		// borra las rutas previas
		String query = "DELETE FROM APP_ROUTE WHERE APP_ID = :appId  ";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(appId));
		q.executeUpdate();

		// Inserta las nuevas routes
		query = "INSERT INTO APP_ROUTE (APP_ID, ROUTE_ID) "
				+ " SELECT  :appId AS APP_ID, ID_ROUTE FROM ROUTE WHERE ID_ROUTE IN ("+ routes + " ) ";
		q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(appId));
		 
		q.executeUpdate();

		// INSERTA LAS RUTAS PADRE QUE NO ESTAN INCLUIDAS
		/*for (int i = 1; i < 4; i++) {
			query = "INSERT INTO APP_ROUTE (APP_ID, ROUTE_ID)  SELECT DISTINCT  :appId AS APP_ID, IDPARENT FROM APP_ROUTE R1 INNER JOIN ROUTE ON (ID_ROUTE=R1.ROUTE_ID) WHERE IDPARENT IS NOT NULL AND  R1.APP_ID=:appId "
					+ " AND NOT EXISTS (SELECT 1 FROM APP_ROUTE AR WHERE AR.APP_ID=:appId AND AR.ROUTE_ID= IDPARENT ) ";
			q = entityManager.createNativeQuery(query);
			q.setParameter("appId", new BigInteger(appId));

			q.executeUpdate();
		}*/

	}

}
