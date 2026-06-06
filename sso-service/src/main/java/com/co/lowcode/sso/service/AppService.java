package com.co.lowcode.sso.service;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.co.lowcode.sso.model.Component;
import com.co.lowcode.sso.model.RequestQuery;
import com.co.lowcode.sso.rethinkdb.RethinkDBConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;

@Service
public class AppService {

	protected final Logger log = LoggerFactory.getLogger(AppService.class);

	@Autowired
	QueryService qs;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private RethinkDBConnectionFactory connectionFactory;

	private static final RethinkDB r = RethinkDB.r;

	@Transactional
	public Map<String, Object> save(Map<String, Object> app, String username) throws SQLException {
		RequestQuery requestQuery = new RequestQuery();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("NAME", app.get("NAME"));
		params.put("DESCRIPTION", app.get("DESCRIPTION"));
		params.put("CREATEDAT", new Date());
		params.put("OWNER", username);
		requestQuery.setParams(params);
		return qs.insert(requestQuery, "APP");
	}

	public List<Map<String, Object>> getApps(String username) {
		Query q = entityManager.createNativeQuery("SELECT ID_APP, A.NAME, A.DESCRIPTION, A.ROOTSCREEN, A.URI" + " FROM APP A "
				+ " INNER JOIN ROLE_APP RA ON A.ID_APP = RA.APP_ID " + " INNER JOIN ROLE R ON R.ID_ROLE = RA.ROLE_ID "
				+ " INNER JOIN ROLE_USERS RU ON RU.ROLE_ID = R.ID_ROLE "
				+ " INNER JOIN USERS U ON U.ID_USER = RU.USER_ID " + " WHERE U.USERNAME = ?0");
		q.setParameter(0, username);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		return res;
	}

	public List<Map<String, Object>> getAppsByEditor(String username) {
		Query q = entityManager.createNativeQuery("SELECT DISTINCT ID_APP, A.NAME, A.DESCRIPTION, A.ROOTSCREEN"
				+ " FROM APP A " 
				+ " LEFT JOIN APP_USER AU ON AU.APP_ID = A.ID_APP "
				+ " LEFT JOIN APP_GROUPS AG ON A.ID_APP = AG.APP_ID "
				+ "	LEFT JOIN GROUPS G ON G.ID_GROUP = AG.GROUPS_ID "
				+ " LEFT JOIN USER_GROUP UG ON UG.GROUP_ID = G.ID_GROUP "
				+ " LEFT JOIN USERS U ON U.ID_USER = UG.USER_ID OR U.ID_USER = AU.USER_ID" 
				+ " WHERE U.USERNAME = ?0 OR A.OWNER = ?0");
		q.setParameter(0, username);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		return res;
	}

	public List<Map<String, Object>> getScreensByEditor(Long idApp, String username) {
		Query q = entityManager.createNativeQuery(
				"SELECT DISTINCT  S.ID_SCREEN, S.NAME, S.UUID, S.INITCOMPONENT  " + "	FROM APP A " + "	LEFT JOIN APP_SCREEN A_S ON A_S.APP_ID = A.ID_APP "
						+ "	LEFT JOIN SCREEN S ON A_S.SCREEN_ID = S.ID_SCREEN "
						+ "	LEFT JOIN APP_GROUPS AG ON A.ID_APP = AG.APP_ID "
						+ "	LEFT JOIN GROUPS G ON G.ID_GROUP = AG.GROUPS_ID "
						+ "	LEFT JOIN USER_GROUP UG ON UG.GROUP_ID = G.ID_GROUP "
						+ "	LEFT JOIN USERS U ON U.ID_USER = UG.USER_ID "
						+ "	WHERE ((U.USERNAME = ?0 AND A.ID_APP = ?1) OR (A.OWNER = ?0  AND A.ID_APP = ?1))  AND ID_SCREEN IS NOT NULL");
		q.setParameter(0, username);
		q.setParameter(1, idApp);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		return res;
	}

	public List<HashMap> getComponentsByEditor(Long idScreen, String username) throws ConnectException {
		Query q = entityManager.createNativeQuery("SELECT C.UUID " + "	FROM SCREEN S "
				+ "	INNER JOIN SCREEN_COMPONENT SC ON SC.ID_SCREEN = S.ID_SCREEN "
				+ "	INNER JOIN COMPONENT C ON C.ID_COMPONENT = SC.ID_COMPONENT "
				+ "	INNER JOIN APP_SCREEN A_S ON A_S.SCREEN_ID = S.ID_SCREEN "
				+ "	INNER JOIN APP A ON A.ID_APP = A_S.APP_ID " + "	INNER JOIN APP_GROUPS AG ON A.ID_APP = AG.APP_ID "
				+ "	INNER JOIN GROUPS G ON G.ID_GROUP = AG.GROUPS_ID "
				+ "	INNER JOIN USER_GROUP UG ON UG.GROUP_ID = G.ID_GROUP "
				+ "	INNER JOIN USERS U ON U.ID_USER = UG.USER_ID "
				+ "	WHERE ((U.USERNAME = :username AND S.ID_SCREEN = :id_screen) OR (A.OWNER = :username AND S.ID_SCREEN = :id_screen)) AND C.ID_COMPONENT IS NOT NULL");
		q.setParameter("username", username);
		q.setParameter("id_screen", idScreen);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		List<HashMap> response = new ArrayList<>();
		for (Map<String, Object> r : res) {
			response.add(getComponents(r.get("UUID").toString()));
		}
		return response;
	}

	public List<Map<String, Object>> getScreenByEditor(Long idScreen, String username) {
		Query q = entityManager.createNativeQuery("SELECT DISTINCT S.UUID " + "	FROM SCREEN S "
				+ "	INNER JOIN APP_SCREEN A_S ON A_S.SCREEN_ID = S.ID_SCREEN "
				+ "	INNER JOIN APP A ON A.ID_APP = A_S.APP_ID " + "	INNER JOIN APP_GROUPS AG ON A.ID_APP = AG.APP_ID "
				+ "	INNER JOIN GROUPS G ON G.ID_GROUP = AG.GROUPS_ID "
				+ "	INNER JOIN USER_GROUP UG ON UG.GROUP_ID = G.ID_GROUP "
				+ "	INNER JOIN USERS U ON U.ID_USER = UG.USER_ID "
				+ "	WHERE ((U.USERNAME = ?0 AND S.ID_SCREEN = ?1) OR (A.OWNER = ?0 AND S.ID_SCREEN = ?1)) AND ID_SCREEN IS NOT NULL");
		q.setParameter(0, username);
		q.setParameter(1, idScreen);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		return res;
	}

	public List<Map<String, Object>> getScreens(Long idApp, String username) {
		Query q = entityManager.createNativeQuery("SELECT S.ID_SCREEN, S.NAME, S.UUID, S.INITCOMPONENT " + "	FROM APP A "
				+ "	INNER JOIN APP_SCREEN A_S ON A_S.APP_ID = A.ID_APP "
				+ "	INNER JOIN SCREEN S ON A_S.SCREEN_ID = S.ID_SCREEN "
				+ "	INNER JOIN ROLE_SCREEN RS ON S.ID_SCREEN = RS.SCREEN_ID "
				+ "	INNER JOIN ROLE R ON R.ID_ROLE = RS.ROLE_ID "
				+ "	INNER JOIN ROLE_USERS RU ON RU.ROLE_ID = R.ID_ROLE "
				+ "	INNER JOIN USERS U ON U.ID_USER = RU.USER_ID " + "	WHERE U.USERNAME = ?0 AND A.ID_APP = ?1");
		q.setParameter(0, username);
		q.setParameter(1, idApp);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		return res;
	}

	public List<HashMap> getComponents(Long idScreen, String username) throws ConnectException {
		Query q = entityManager.createNativeQuery("SELECT C.UUID " + "	FROM SCREEN S "
				+ "	INNER JOIN SCREEN_COMPONENT SC ON SC.ID_SCREEN = S.ID_SCREEN "
				+ "	INNER JOIN COMPONENT C ON C.ID_COMPONENT = SC.ID_COMPONENT "
				+ "	INNER JOIN ROLE_SCREEN RS ON S.ID_SCREEN = RS.SCREEN_ID "
				+ "	INNER JOIN ROLE R ON R.ID_ROLE = RS.ROLE_ID "
				+ "	INNER JOIN ROLE_USERS RU ON RU.ROLE_ID = R.ID_ROLE "
				+ "	INNER JOIN USERS U ON U.ID_USER = RU.USER_ID "
				+ "	WHERE U.USERNAME = :username AND S.ID_SCREEN = :id_screen ");
		q.setParameter("username", username);
		q.setParameter("id_screen", idScreen);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		List<HashMap> response = new ArrayList<>();
		for (Map<String, Object> r : res) {
			response.add(getComponents(r.get("uuid").toString()));
		}
		return response;
	}

	public Map<String, Object> getComponentByEditor(String componentId, String username) {
		Query q = entityManager.createNativeQuery("SELECT C.* FROM COMPONENT C "
				+ "						INNER JOIN SCREEN_COMPONENT SC ON SC.ID_COMPONENT = C.ID_COMPONENT "
				+ "						INNER JOIN SCREEN S ON SC.ID_SCREEN = S.ID_SCREEN "
				+ "						INNER JOIN APP_SCREEN A_S ON A_S.SCREEN_ID = S.ID_SCREEN "
				+ "						INNER JOIN APP A ON A.ID_APP = A_S.APP_ID "
				+ "						INNER JOIN APP_GROUPS AG ON A.ID_APP = AG.APP_ID "
				+ "						INNER JOIN GROUPS G ON G.ID_GROUP = AG.GROUPS_ID "
				+ "						INNER JOIN USER_GROUP UG ON UG.GROUP_ID = G.ID_GROUP "
				+ "						INNER JOIN USERS U ON U.ID_USER = UG.USER_ID "
				+ "					WHERE (U.USERNAME =  :username AND C.UUID = :componentId) "
				+ "						OR (A.OWNER = :username AND C.UUID = :componentId)");

		q.setParameter("username", username);
		q.setParameter("componentId", componentId);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();

		if (res.size() > 0) {
			return res.get(0);
		}
		return null;
	}

	public HashMap getComponents(String uuid) throws java.net.ConnectException {
		Connection conn = connectionFactory.createConnection();
		HashMap cursor = r.db("lowcode").table("component").get(uuid).run(conn);
		conn.close();
		return cursor;
	}

	public HashMap getStructure(String uuid) throws java.net.ConnectException {
		Connection conn = connectionFactory.createConnection();
		HashMap cursor = r.db("lowcode").table("structure").get(uuid).run(conn);
		conn.close();

		return cursor;
	}

	public HashMap getApiDefinition(String uuid) throws java.net.ConnectException {
		Connection conn = connectionFactory.createConnection();
		HashMap cursor = r.db("lowcode").table("apiDefinition").get(uuid).run(conn);
		conn.close();
		return cursor;
	}

	public HashMap getFlowDefinition(String uuid) throws java.net.ConnectException {
		Connection conn = connectionFactory.createConnection();
		HashMap cursor = r.db("lowcode").table("flowDefinition").get(uuid).run(conn);
		conn.close();
		return cursor;
	}

	public HashMap getDataModel(String uuid) throws java.net.ConnectException {
		Connection conn = connectionFactory.createConnection();
		HashMap cursor = r.db("lowcode").table("dataModel").get(uuid).run(conn);
		conn.close();
		return cursor;
	}

	public HashMap getStyle(String uuid) throws java.net.ConnectException {
		Connection conn = connectionFactory.createConnection();
		HashMap cursor = r.db("lowcode").table("style").get(uuid).run(conn);
		conn.close();
		return cursor;
	}
	
	public void cloneChildren(Component component) {
		
		
	}
	

	public Component saveComponent(String brickId, Long screenId, String parentId, Integer insertPosition,
			String username) throws java.net.ConnectException {
		List<Map<String, Object>> screens = getScreenByEditor(screenId, username);
		if (screens.size() > 0) {
			
			String uuidScreen = screens.get(0).get("UUID").toString();
			
			Connection conn = connectionFactory.createConnection();
			
			HashMap cursor = r.db("lowcode").table("brick").get(brickId).run(conn);
			
			final ObjectMapper mapper = new ObjectMapper(); 
			final Component component = mapper.convertValue(cursor, Component.class);
			component.setScreenId(uuidScreen);
			component.setId(null);
			component.setChildren(new ArrayList());
			HashMap run = r.db("lowcode").table("component").insert(component).run(conn);
			String uuid = ((java.util.ArrayList) run.get("generated_keys")).get(0).toString();
			log.info("Insert {}", uuid);
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("UUID", uuid);

			try {
				Map<String, Object> idComponent = qs.create(params, "COMPONENT", "ID_COMPONENT");
				RequestQuery rq = new RequestQuery();
				params = new LinkedHashMap<>();
				params.put("ID_COMPONENT", idComponent.get("ID_COMPONENT"));
				params.put("ID_SCREEN", screenId);
				rq.setParams(params);
				qs.insert(rq, "SCREEN_COMPONENT");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(parentId != null) {
				HashMap run2 = r.db("lowcode").table("component").filter(r.hashMap("id", parentId)).update(
						row -> r.hashMap("children", row.g("children").default_(r.array()).insertAt(insertPosition, uuid)))
						.run(conn);

				log.info("Update {}", run2);
			}
			conn.close();
			if(cursor.get("children") != null) {
				int i = 0;
				for(String child : (List<String>)cursor.get("children")) {
					saveComponent(child, screenId, uuid, i, username);
					i++;
				}
			}
			return component;
		}
		return null;
	}

	@Transactional
	public void removeComponent(Long screenId, String componentId, String parentId, Integer position, String username)
			throws ConnectException {
		Map<String, Object> component = getComponentByEditor(componentId, username);
		if (component != null) {
			Connection conn = connectionFactory.createConnection();
			HashMap run = r.db("lowcode").table("component").filter(r.hashMap("id", componentId)).delete()
					.run(conn);

			log.info("Delete {}", run);
			

			String query = "DELETE FROM SCREEN_COMPONENT  WHERE ID_COMPONENT = :componentId AND ID_SCREEN = :screenId";
			Query q = entityManager.createNativeQuery(query);
			q.setParameter("componentId", component.get("id_component"));
			q.setParameter("screenId", screenId);
			q.executeUpdate();

			query = "DELETE FROM COMPONENT  WHERE UUID = :componentId";
			q = entityManager.createNativeQuery(query);
			q.setParameter("componentId", componentId);
			q.executeUpdate();

			HashMap run2 = r.db("lowcode").table("component").filter(r.hashMap("id", parentId))
					.update(row -> r.hashMap("children", row.g("children").default_(r.array()).deleteAt(position)))
					.run(conn);

			log.info("Delete {}", run2);
			conn.close();

		}

	}
	
	public boolean grantedApp(String username, Integer appId) {
		Query q = entityManager.createNativeQuery(" SELECT COUNT(*) AS C FROM APP A "
				+ "			 LEFT JOIN APP_USER AU ON AU.APP_ID = A.ID_APP "
				+ "			 LEFT JOIN APP_GROUPS AG ON A.ID_APP = AG.APP_ID "
				+ "			 LEFT JOIN GROUPS G ON G.ID_GROUP = AG.GROUPS_ID "
				+ "			 LEFT JOIN USER_GROUP UG ON AG.GROUPS_ID = G.ID_GROUP "
				+ "			 LEFT JOIN USERS U ON U.ID_USER = UG.USER_ID OR U.ID_USER = AU.USER_ID "
				+ "			 WHERE (A.OWNER = ?0 AND A.ID_APP = ?1) OR "
				+ "			  (U.USERNAME = ?0 AND UG.PROFILE = 'ADMIN' AND A.ID_APP = ?1) OR "
				+ "			  (U.USERNAME = ?0 AND AU.APP_ID = ?1 AND AU.PROFILE = 'ADMIN')");
		q.setParameter(0, username);
		q.setParameter(1, appId);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		Map<String,Object> map = res.get(0);
		return ((Integer) map.get("C")) > 0 ? true : false;
	}
	
	@Transactional
	public void bindAppUser(Map<String, Object> appuser, String username) {
		if(grantedApp(username, ((Integer)  appuser.get("APP_ID")))) {
			RequestQuery requestQuery = new RequestQuery();
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("APP_ID", appuser.get("APP_ID"));
			params.put("USER_ID", appuser.get("USER_ID"));
			params.put("CREATED_AT", new Date());
			requestQuery.setParams(params);
			try {
				qs.insert(requestQuery, "USER_GROUP");
			} catch (SQLException e) {
				throw new RuntimeException("El usuario ya está vinculado");
			}
		}
		else {
			throw new RuntimeException("Usuario no tiene permiso para esta acción");
		}
	}

}
