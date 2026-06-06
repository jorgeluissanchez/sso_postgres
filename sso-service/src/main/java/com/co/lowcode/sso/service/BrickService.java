package com.co.lowcode.sso.service;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.co.lowcode.sso.rethinkdb.RethinkDBConnectionFactory;
import com.rethinkdb.RethinkDB;

@Service
public class BrickService {

	protected final Logger log = LoggerFactory.getLogger(BrickService.class);

	@Autowired
	QueryService qs;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private RethinkDBConnectionFactory connectionFactory;

	private static final RethinkDB r = RethinkDB.r;


	public List<Map<String, Object>> getBricks() {
		Query q = entityManager.createNativeQuery("SELECT ID_BUILDING_BRICK, LABEL FROM BUILDING_BRICK ");
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		for(Map<String, Object> r : res) {
			q = entityManager.createNativeQuery("SELECT UUID, LABEL, ICON  FROM OPTION_BRICK "
											+ " WHERE ID_BUILDING_BRICK = :building");
			q.setParameter("building", r.get("ID_BUILDING_BRICK"));
			hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
			hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			List<Map<String, Object>> result = hibernateQuery.list();
			r.put("OPTION_BRICK", result);
		}
		return res;
	}
	


}
