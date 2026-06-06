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
import com.co.lowcode.sso.exception.RoleDuplicateException;
import com.co.lowcode.sso.model.RequestQuery;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;


@Service
public class RoleService {

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
	public Map<String, Object> createRole(Map<String, Object> model) throws TemplateNotFoundException,

			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException,
			SQLException {
		if (!existRole(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("NAME", model.get("NAME"));
			 

			rq.setParams(params);

			Map<String, Object> createdMs = queryService.create(params, "ROLE", "ID_ROLE");

			return createdMs;

		} else {
			throw new RoleDuplicateException(model.get("NAME").toString());
		}

	}

	@Transactional	
	public Map<String, Object> updateRole(Map<String, Object> model) throws TemplateNotFoundException,

			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException,
			SQLException {
		if (!existRole(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("NAME", model.get("NAME"));
			rq.setParams(params);
			
			Map<String, Object> where = new LinkedHashMap<String, Object>(); 
			where.put("ID_ROLE", model.get("ID_ROLE"));
			rq.setWhere(where);
			
			Map<String, Object> updatedMs = queryService.update(rq, "ROLE");

			return updatedMs;

		} else {
			throw new RoleDuplicateException(model.get("NAME").toString());
		}

	}

	public List<Map<String, Object>> getRoles() {
		String query = "SELECT ID_ROLE \"ID_ROLE\", NAME \"NAME\" FROM ROLE ORDER BY NAME ";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	public List<Map<String, Object>> getRolesOwn(String username) {
		String query = "SELECT R.ID_ROLE \"ID_ROLE\", R.NAME \"NAME\" FROM "
				+ " SSO_PRUEBAS.dbo.role R "
				+ "INNER JOIN SSO_PRUEBAS.dbo.role_users RU ON RU.role_id = R.id_role "
				+ "INNER JOIN SSO_PRUEBAS.dbo.users U ON RU.user_id = U.id_user "
				+ "WHERE username =:username AND R.NAME <> 'ADMIN_USUARIOS_OPERADORAS'";
		
		RequestQuery requestQuery = new RequestQuery();
		Map<String,Object> params = new LinkedHashMap<String, Object>();
		params.put("username", username);
		requestQuery.setParams(params);
		
		return queryService.getResultQuery(query, requestQuery);
	}
	
	public List<Map<String, Object>> getRolesUserChecked(String userId, String username) {
		String query = "WITH CTE AS (SELECT id_role, name, "
				+ "(CASE WHEN RU.USER_ID = :userId THEN 1 ELSE 0 END) AS CHECKED   FROM role R   "
				+ "INNER JOIN role_users RU ON RU.role_id = R.id_role   "
				+ "INNER JOIN users U ON RU.user_id = U.id_user   "
				+ "WHERE (username = :username OR user_id = :userId) AND R.NAME <> 'ADMIN_USUARIOS_OPERADORAS') "
				+ "SELECT ID_ROLE, NAME AS ROLNAME, CHECKED FROM "
				+ "(   SELECT id_role, name, CHECKED,  ROW_NUMBER() "
				+ "OVER (PARTITION BY id_role, name ORDER BY CHECKED DESC) AS rn   FROM CTE ) AS Subquery "
				+ "WHERE rn = 1";
		
		RequestQuery requestQuery = new RequestQuery();
		Map<String,Object> params = new LinkedHashMap<String, Object>();
		params.put("username", username);
		params.put("userId", new BigInteger(userId));
		requestQuery.setParams(params);
		
		return queryService.getResultQuery(query, requestQuery);
	}
	
	
	
	
	public List<Map<String, Object>> getUsersRole(String roleId){
		
		String query = "SELECT ID_USER \"ID_USER\", ID_ROLE \"ID_ROLE\", NAME \"ROLNAME\", FULLNAME \"FULLNAME\","
				+ " USERNAME \"EMAIL\"   "
				+ " FROM ROLE INNER JOIN ROLE_USERS  ON ( ROLE_ID= ID_ROLE ) " 
				+ " INNER JOIN USERS ON  ( USER_ID=ID_USER  ) "
				+ " WHERE ROLE_ID =:roleId   "
				+ " ORDER BY NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	
	public List<Map<String, Object>> getUsersRoleChecked(String roleId) {
		
		
		String query = "SELECT :roleId \"ID_ROLE\", (CASE WHEN EM.ROLE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_USER \"ID_USER\", FULLNAME \"FULLNAME\", USERNAME \"EMAIL\"   "
				+ " , (SELECT NAME FROM ROLE WHERE ID_ROLE= :roleId ) \"ROLNAME\" "
				+ " FROM USERS LEFT JOIN (SELECT ROLE_ID, USER_ID FROM ROLE_USERS WHERE ROLE_ID =:roleId ) EM  ON ( USER_ID= ID_USER )  "
				+ " ORDER BY USERNAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	

	public List<Map<String, Object>> getEndpointsRoleChecked(String roleId) {
		
		String query = "SELECT :roleId \"ID_ROLE\", (CASE WHEN EM.ROLE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_ENDPOINT \"ID_ENDPOINT\", METHOD \"METHOD\", NUMBERPARAMS \"NUMBERPARAMS\", PATH \"PATH\""
				+ " , (SELECT NAME FROM ROLE WHERE ID_ROLE= :roleId ) \"ROLE\" "
				+ " FROM ENDPOINT LEFT JOIN (SELECT ROLE_ID, ENDPOINT_ID FROM ROLE_ENDPOINT WHERE ROLE_ID =:roleId ) EM  ON ( ENDPOINT_ID=ID_ENDPOINT )  "
				+ " ORDER BY \"METHOD\",\"PATH\"  ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}


	
	public List<Map<String, Object>> getEndpointsRole(String roleId) {
		
		String query = "SELECT  ID_ROLE \"ID_ROLE\", NAME \"ROLE\", ID_ENDPOINT \"ID_ENDPOINT\","
				+ " METHOD \"METHOD\", NUMBERPARAMS \"NUMBERPARAMS\", PATH \"PATH\" "
				+ " FROM ENDPOINT INNER JOIN  ROLE_ENDPOINT ON (ENDPOINT_ID=ID_ENDPOINT) "
				+ " INNER JOIN ROLE ON ( ID_ROLE=ROLE_ID )   "
				+ " WHERE ROLE_ID =:roleId AND    "
				+ " ORDER BY \"METHOD\",\"PATH\"";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

	
	
	public List<Map<String, Object>> getRoutesRoleChecked(String roleId) {
		
		String query = "SELECT :roleId \"ROLE_ID\", (CASE WHEN EM.ROLE_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_ROUTE \"ROUTE_ID\", RT.NAME  \"ROUTE_NAME\", COALESCE(IDPARENT,0) AS \"ROUTE_PARENT\", PATH AS \"ROUTE_PATH\" "
				+ " , (SELECT NAME FROM ROLE WHERE ID_ROLE= :roleId ) \"ROLE_NAME\" "
				+ " FROM ROUTE RT LEFT JOIN (SELECT ROLE_ID, ROUTE_ID FROM ROLE_ROUTE WHERE ROLE_ID =:roleId ) EM  ON ( ROUTE_ID=ID_ROUTE )  "
				+ " ORDER BY RT.NAME  ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}


	
	public List<Map<String, Object>> getRoutesRole(String roleId) {
		
		String query = "SELECT  ROLE_ID \"ROLE_ID\", RL.NAME \"ROLE_NAME\", ROUTE_ID \"ROUTE_ID\", RT.NAME \"ROUTE_NAME\", COALESCE(IDPARENT,0) \"ROUTE_PARENT\",  PATH \"ROUTE_PATH\"  "
				+ " FROM ROUTE RT INNER JOIN  ROLE_ROUTE ON (ROUTE_ID=ID_ROUTE) "
				+ " INNER JOIN ROLE RL ON ( ID_ROLE=ROLE_ID )   "
				+ " WHERE ROLE_ID =:roleId AND    "
				+ " ORDER BY RT.NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", roleId);

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}

	
	

	public Boolean existEndpointRole(Map<String, Object> model) {
		String query = "SELECT 1  FROM ROLE_ENDPOINT WHERE ENDPOINT_ID = :endpointId AND ROLE_ID =:roleId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("endpointId", model.get("ID_ENDPOINT"));
		q.setParameter("roleId", model.get("ID_ROLE"));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}
	
	public Boolean existUserRole(Map<String, Object> model) {
		String query = "SELECT 1  FROM ROLE_USERS WHERE USER_ID = :userId AND ROLE_ID =:roleId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("userId", model.get("ID_USER"));
		q.setParameter("roleId", model.get("ID_ROLE"));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}
	

	public Boolean existRole(Map<String, Object> model) {
		String query = "SELECT 1  FROM ROLE WHERE NAME = :name";

		if (model.get("ID_ROLE") != null && model.get("ID_ROLE") != "0") {
			query += " AND ID_ROLE !=:roleId ";
		}

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("name", model.get("NAME"));

		if (model.get("ID_ROLE") != null && model.get("ID_ROLE") != "0") {
			q.setParameter("roleId", model.get("ID_ROLE"));
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
	public Map<String, Object> addEndpoint(Map<String, Object> model) throws SQLException {

		if (!existEndpointRole(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("ENDPOINT_ID", model.get("ID_ENDPOINT"));
			params.put("ROLE_ID", model.get("ID_ROLE"));
			rq.setParams(params);

			Map<String, Object> createdEndpointMS = queryService.create(params, "ROLE_ENDPOINT", "ENDPOINT_ID");

			return createdEndpointMS;
		}
		return null;

	}

	
	@Transactional
	public Map<String, Object> addUser(Map<String, Object> model) throws SQLException {

		if (!existUserRole(model)) {

			RequestQuery rq = new RequestQuery();

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("USER_ID", model.get("ID_USER"));
			params.put("ROLE_ID", model.get("ID_ROLE"));
			rq.setParams(params);

			Map<String, Object> createdEndpointMS = queryService.create(params, "ROLE_USERS", "USER_ID");

			return createdEndpointMS;
		}
		return null;

	}


	@Transactional
	public void removeEndpoint(String roleId, String endpointId) throws SQLException {

		String query = "DELETE FROM ROLE_ENDPOINT WHERE ENDPOINT_ID = :endpointId AND ROLE_ID =:roleId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("endpointId", new BigInteger(endpointId));
		q.setParameter("roleId", new BigInteger(roleId));
		q.executeUpdate();

	}

	@Transactional
	public void removeRole(String roleId, String userId) throws SQLException {

		String query = "DELETE FROM ROLE_USERS WHERE USER_ID = :userId AND ROLE_ID =:roleId";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("userId", new BigInteger(userId));
		q.setParameter("roleId", new BigInteger(roleId));
		q.executeUpdate();

	}
	
	
	
	
	
	@Transactional
	public void saveRoutes(String roleId, String routes) throws SQLException {

		//borra las rutas previas
		String query = "DELETE FROM 	ROLE_ROUTE WHERE ROLE_ID = :roleId  ";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));
		q.executeUpdate();
		
		
		//Inserta las nuevas routes
		query = "INSERT INTO ROLE_ROUTE (ROLE_ID, ROUTE_ID)  SELECT  :roleId AS ROLE_ID, ID_ROUTE FROM ROUTE WHERE ID_ROUTE IN ( " + routes + ") ";
		q = entityManager.createNativeQuery(query);
		q.setParameter("roleId", new BigInteger(roleId));
		 
		
		q.executeUpdate();
		
		//INSERTA LAS RUTAS PADRE QUE NO ESTAN INCLUIDAS
		for(int i=1;i<4;i++){
			query = "INSERT INTO ROLE_ROUTE (ROLE_ID, ROUTE_ID)  SELECT DISTINCT  :roleId AS ROLE_ID, IDPARENT FROM ROLE_ROUTE R1 INNER JOIN ROUTE ON (ID_ROUTE=R1.ROUTE_ID) WHERE IDPARENT IS NOT NULL AND  R1.ROLE_ID=:roleId "
					+ " AND NOT EXISTS (SELECT 1 FROM ROLE_ROUTE RR WHERE RR.ROLE_ID=:roleId AND RR.ROUTE_ID= IDPARENT ) ";
			q = entityManager.createNativeQuery(query);
			q.setParameter("roleId", new BigInteger(roleId));
			q.executeUpdate();
		}
	}
}