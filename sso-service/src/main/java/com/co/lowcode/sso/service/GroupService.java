package com.co.lowcode.sso.service;

import java.sql.SQLException;
import java.util.Date;
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

import com.co.lowcode.sso.model.RequestQuery;

@Service
public class GroupService {

	protected final Logger log = LoggerFactory.getLogger(GroupService.class);

	@Autowired
	QueryService qs;

	@PersistenceContext
	private EntityManager entityManager;


	@Transactional
	public Map<String, Object> save(Map<String, Object> group, String username) throws SQLException {
		RequestQuery requestQuery = new RequestQuery();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("NAME", group.get("NAME"));
		params.put("CREATED_AT", new Date());
		params.put("OWNER", username);
		requestQuery.setParams(params);
		return qs.insert(requestQuery, "GROUPS");
	}
	
	
	public List<Map<String, Object>> getGroupsByEditor(String username) {
		Query q = entityManager.createNativeQuery("SELECT G.* FROM GROUPS G "
				+ " LEFT JOIN USER_GROUP UG ON G.ID_GROUP = G.ID_GROUP "
				+ " LEFT JOIN USERS U ON U.ID_USER = UG.USER_ID "
				+ " WHERE (OWNER = ?0) OR (U.USERNAME = ?0 AND UG.profile = 'ADMIN')");
		q.setParameter(0, username);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		return res;
	}
	
	public boolean grantedGroup(String username, Integer groupId) {
		Query q = entityManager.createNativeQuery(" SELECT COUNT(*) as GROUPS FROM GROUPS G "
				+ " LEFT JOIN USER_GROUP UG ON G.ID_GROUP = G.ID_GROUP "
				+ " LEFT JOIN USERS U ON U.ID_USER = UG.USER_ID "
				+ " WHERE (OWNER = ?0 AND G.ID_GROUP = ?1) OR "
				+ "       (U.USERNAME = ?0 AND UG.profile = 'ADMIN' AND G.ID_GROUP = ?1)");
		q.setParameter(0, username);
		q.setParameter(1, groupId);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		Map<String,Object> map = res.get(0);
		return ((Integer) map.get("GROUPS")) > 0 ? true : false;
	}
	
	
	@Transactional
	public void bindUserGroup(Map<String, Object> group,String username) {
		if (grantedGroup(username, ((Integer) group.get("GROUP_ID")))) {
			RequestQuery requestQuery = new RequestQuery();
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("GROUP_ID", group.get("GROUP_ID"));
			params.put("USER_ID", group.get("USER_ID"));
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
	
	@Transactional
	public void bindAppGroup(Map<String, Object> group, String username) throws SQLException{
		if (grantedGroup(username, ((Integer) group.get("GROUP_ID")))) {
			RequestQuery requestQuery = new RequestQuery();
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("GROUP_ID", group.get("GROUP_ID"));
			params.put("APP_ID", group.get("APP_ID"));
			requestQuery.setParams(params);
			qs.insert(requestQuery, "APP_GROUPS");
		}
		else {
			throw new RuntimeException("Usuario no tiene permiso para esta acción");
		}
	}
	
	
	
}
