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

import com.co.lowcode.sso.exception.RouteDuplicateException;
import com.co.lowcode.sso.model.RequestQuery;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;


@Service
public class RouteService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private QueryService queryService;

	@Autowired
	RabbitMQSender rabbitMQSender;


	@Transactional	
	public Map<String, Object> createRoute(Map<String, Object> model) throws TemplateNotFoundException,

			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException,
			SQLException {
		
		if (!existRoute(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("NAME", model.get("NAME"));
			params.put("ICON", model.get("ICON"));
			params.put("PATH", model.get("PATH"));
			params.put("MENUORDER", Integer.parseInt(model.get("MENUORDER").toString()));
			params.put("TYPE", model.get("TYPE"));
		//	params.put("COMPONENT_ID_COMPONENT", model.get("ID_COMPONENT"));
			
			if(!model.get("IDPARENT").equals("0"))
				params.put("IDPARENT", new BigInteger( model.get("IDPARENT").toString()));
			 

			rq.setParams(params);

			Map<String, Object> createdMs = queryService.create(params, "ROUTE", "ID_ROUTE");

			return createdMs;

		} else {
			throw new RouteDuplicateException(model.get("NAME").toString());
		}

	}

	@Transactional	
	public Map<String, Object> updateRoute(Map<String, Object> model) throws TemplateNotFoundException,

			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException,
			SQLException {
		if (!existRoute(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("NAME", model.get("NAME"));
			params.put("ICON", model.get("ICON"));
			params.put("PATH", model.get("PATH"));
			params.put("MENUORDER", Integer.parseInt( model.get("MENUORDER").toString()));
			params.put("TYPE", model.get("TYPE"));
		//	params.put("COMPONENT_ID_COMPONENT", model.get("ID_COMPONENT"));
			
			
			if( model.get("IDPARENT")!=null && !model.get("IDPARENT").equals("0"))
				params.put("IDPARENT", model.get("IDPARENT"));
			
			Map<String, Object> where = new LinkedHashMap<String, Object>(); 
			where.put("ID_ROUTE", model.get("ID_ROUTE"));
			rq.setWhere(where);
			

			rq.setParams(params);

			Map<String, Object> updatedMs = queryService.update(rq, "ROUTE");

			return updatedMs;

		} else {
			throw new RouteDuplicateException(model.get("NAME").toString());
		}

	}

	public List<Map<String, Object>> getRoutes() {
		String query = "SELECT ID_ROUTE,ICON,IDPARENT,MENUORDER,NAME,PATH,TYPE,COMPONENT_ID_COMPONENT ID_COMPONENT "
				+ " FROM ROUTE   ORDER BY NAME  ";
		
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	public List<Map<String, Object>> getRoutesByParent(String parentId) {
		
		String query = "SELECT ID_ROUTE \"ID_ROUTE\", ICON \"ICON\","
				+ " IDPARENT \"IDPARENT\", MENUORDER \"MENUORDER\","
				+ " NAME \"NAME\" ,PATH \"PATH\", TYPE \"TYPE\""
				+ " FROM ROUTE WHERE IDPARENT=0 OR IDPARENT IS NULL ORDER BY NAME  ";
		
		if(!parentId.equals("0") && !parentId.equals("") ) {
			query = "SELECT ID_ROUTE \"ID_ROUTE\", ICON \"ICON\", IDPARENT \"IDPARENT\","
					+ " MENUORDER \"MENUORDER\", NAME \"NAME\", PATH \"PATH\", TYPE \"TYPE\""
					+ " FROM ROUTE WHERE IDPARENT=:parentId ORDER BY NAME  ";
		}
		
		Query q = entityManager.createNativeQuery(query);
		if(!parentId.equals("0") && !parentId.equals("") ) {
			q.setParameter("parentId", new BigInteger(parentId));
		}

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();
		
	}
	
	
	public List<Map<String, Object>> getAppsRoute(String routeId){
		
		String query = "SELECT APP_ID \"APP_ID\", ROUTE_ID \"ROUTE_ID\", RT.NAME ROUTE_APP \"ROUTE_APP\","
				+ " APP.NAME APP_NAME \"APP_NAME\", RT.IDPARENT ROUTE_PARENT \"ROUTE_PARENT\" "
				+ " FROM ROUTE RT INNER JOIN APP_ROUTE  ON ( ROUTE_ID= ID_ROUTE ) " 
				+ " INNER JOIN APP ON  ( APP_ID=ID_APP ) "
				+ " WHERE ROUTE_ID =:routeId   "
				+ " ORDER BY APP.NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("routeId", new BigInteger(routeId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	
	public List<Map<String, Object>> getAppsRouteChecked(String routeId) {
		
		String query = "SELECT :routeId \"ROUTE_ID\", (CASE WHEN EM.ROUTE_ID IS NULL THEN 0 ELSE 1 END) \"CHECKED\","
				+ " ID_APP \"APP_ID\", NAME  \"APP_NAME\",  "
				+ "  (SELECT NAME FROM ROUTE WHERE ID_ROUTE= :routeId )  \"ROUTE_NAME\" "
				+ " FROM APP LEFT JOIN (SELECT ROUTE_ID, APP_ID FROM APP_ROUTE WHERE ROUTE_ID =:routeId ) EM  ON ( APP_ID= ID_APP )  "
				+ " ORDER BY NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("routeId", new BigInteger(routeId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	

	public List<Map<String, Object>> getRolesRouteChecked(String routeId) {
		
		String query = "SELECT :routeId \"ROUTE_ID\", "
				+ "(CASE WHEN EM.ROLE_ID IS NULL THEN 0 ELSE 1 END) \"CHECKED\","
				+ " ID_ROLE  \"ROLE_ID\", NAME \"ROLE_NAME\",  "
				+ "  (SELECT NAME FROM ROUTE WHERE ID_ROUTE= :routeId ) \"ROUTE_NAME\" "
				+ " FROM ROLE LEFT JOIN (SELECT ROLE_ID, ROUTE_ID FROM ROLE_ROUTE WHERE ROUTE_ID =:routeId ) EM  ON ( ROLE_ID=ID_ROLE )  "
				+ " ORDER BY NAME   ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("routeId", new BigInteger(routeId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}


	
	public List<Map<String, Object>> getRolesRoute(String routeId) {
		
		String query = "SELECT  ROLE_ID \"ROLE_ID\", RL.NAME \"ROLE_NAME\", "
				+ "ROUTE_ID \"ROUTE_ID\", RT.NAME \"ROUTE_NAME\", IDPARENT \"ROUTE_PARENT\"  "
				+ " FROM ROUTE RT INNER JOIN  ROLE_ROUTE ON (ROUTE_ID=ID_ROUTE) "
				+ " INNER JOIN ROLE RL ON ( ID_ROLE=ROLE_ID )   "
				+ " WHERE ROUTE_ID =:routeId AND    "
				+ " ORDER BY RL.NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("routeId", new BigInteger(routeId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

	

	public Boolean existRoleRoute(Map<String, Object> model) {
		String query = "SELECT 1  FROM ROLE_ROUTE WHERE ROLE_ID = :roleId AND ROUTE_ID =:routeId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", model.get("ROLE_ID"));
		q.setParameter("routeId", model.get("ROUTE_ID"));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}
	
	public Boolean existAppRoute(Map<String, Object> model) {
		String query = "SELECT 1  FROM APP_ROUTE WHERE APP_ID = :appId AND ROUTE_ID =:routeId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", model.get("APP_ID"));
		q.setParameter("routeId", model.get("ROUTE_ID"));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}
	

	public Boolean existRoute(Map<String, Object> model) {
		String query = "SELECT 1  FROM ROUTE WHERE NAME = :name and PATH=:path";

		if (model.get("ID_ROUTE") != null && model.get("ID_ROUTE") != "0") {
			query += " AND ID_ROUTE !=:routeId ";
		}

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("name", model.get("NAME"));
		q.setParameter("path", model.get("PATH"));

		if (model.get("ID_ROUTE") != null && model.get("ID_ROUTE") != "0") {
			q.setParameter("routeId", model.get("ID_ROUTE"));
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
	public Map<String, Object> addRole(Map<String, Object> model) throws SQLException {

		if (!existRoleRoute(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("ROLE_ID", model.get("ROLE_ID"));
			params.put("ROUTE_ID", model.get("ROUTE_ID"));
			rq.setParams(params);

			Map<String, Object> createdRow = queryService.create(params, "ROLE_ROUTE", "ROLE_ID");

			return createdRow;
		}
		return null;

	}

	
	@Transactional
	public Map<String, Object> addApp(Map<String, Object> model) throws SQLException {

		if (!existAppRoute(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("APP_ID", model.get("APP_ID"));
			params.put("ROUTE_ID", model.get("ROUTE_ID"));
			rq.setParams(params);

			Map<String, Object> createdRow = queryService.create(params, "APP_ROUTE", "APP_ID");

			return createdRow;
		}
		return null;

	}


	@Transactional
	public void removeRole(String routeId, String roleId) throws SQLException {

		String query = "DELETE FROM ROLE_ROUTE WHERE ROLE_ID = :roleId AND ROUTE_ID =:routeId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));
		q.setParameter("routeId", new BigInteger( routeId));
		q.executeUpdate();

	}

	@Transactional
	public void removeApp(String routeId, String appId) throws SQLException {

		String query = "DELETE FROM APP_ROUTE WHERE APP_ID = :appId AND ROUTE_ID =:routeId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("appId", new BigInteger(appId));
		q.executeUpdate();

	}

	
}
