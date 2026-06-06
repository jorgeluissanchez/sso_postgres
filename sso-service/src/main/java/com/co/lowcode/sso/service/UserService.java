package com.co.lowcode.sso.service;

import java.io.IOException;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.co.lowcode.sso.config.GeneralConfig;
import com.co.lowcode.sso.config.SmtpMailSender;
import com.co.lowcode.sso.exception.EmailInvalidException;
import com.co.lowcode.sso.exception.TokenExpiredException;
import com.co.lowcode.sso.exception.UserDuplicateException;
import com.co.lowcode.sso.exception.UserNotFoudException;
import com.co.lowcode.sso.model.RequestQuery;
import com.co.lowcode.sso.util.Util;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;




@Service
public class UserService {

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
	private SmtpMailSender smtpMailSender;
	

	
	
	@SuppressWarnings("unchecked")
	@Transactional
	public Map<String, Object> createAccount(Map<String, Object> user, String roleName, String pathTemplate) throws TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		//BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();
		if (!existUser(user)) {
			String tokenActivation =UUID.randomUUID().toString();
			RequestQuery rq = new RequestQuery();
			
			Boolean ldap = false;
			
			if(user.get("LDAP")==null) {
				ldap = false;
			}else if(user.get("LDAP").equals(1) || user.get("LDAP").equals(true) ) {
				ldap = true;
			}
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("USERNAME", user.get("USERNAME"));
			params.put("FULLNAME", user.get("FULLNAME"));
			params.put("LDAP", ldap);
			params.put("ACTIVE", ldap);
			params.put("TOKENACTIVATION", tokenActivation);
			//params.put("PASSWORD", bcryptEncoder.encode("SSO*2023*"));
			user.put("TOKENACTIVATION", tokenActivation);
			
			rq.setParams(params);
			if (!ldap) {
				if (Util.validateEmailAddress(user.get("USERNAME").toString())) {
					sendMailActivation(user, pathTemplate);
				} else {
					throw new EmailInvalidException();
				}
			}
			
			Map<String, Object> createdUser = queryService.create(params, "USERS", "ID_USER");
			params = new LinkedHashMap<>();
			params.put("USER_ID", createdUser.get("ID_USER"));
			if (!(user.get("ID_ROLE").toString().equals("-1"))) {
				params.put("ROLE_ID", user.get("ID_ROLE"));
				queryService.create(params, "ROLE_USERS", "USER_ID");
			} else if (((List<Map<String,Object>>)user.get("ID_ROLES")) != null){
				for(int i = 0; i < ((List<Map<String,Object>>)user.get("ID_ROLES")).size(); i++) {
					params.put("ROLE_ID", ((List<Map<String,Object>>)user.get("ID_ROLES")).get(i).get("ID_ROLE"));
					queryService.create(params, "ROLE_USERS", "USER_ID");
				}
			}
			return createdUser;
		} else {
			throw new UserDuplicateException(user.get("USERNAME").toString());
		}
	}
	
	@Transactional
	public Map<String, Object> updateAccount(Map<String, Object> user, String roleName) throws TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		
		if (existUser(user)) {
			RequestQuery rq = new RequestQuery();
			Boolean active = user.get("ACTIVE")==null?false:  ( user.get("ACTIVE").equals("true") ||  user.get("ACTIVE").equals(1) );
			Boolean ldap = user.get("LDAP")==null?false:  ( user.get("LDAP").equals("true") ||  user.get("LDAP").equals(1) );
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("USERNAME", user.get("USERNAME"));
			params.put("FULLNAME", user.get("FULLNAME"));
			params.put("LDAP", ldap);
			params.put("ACTIVE", active);
			rq.setParams(params);
			
			Map<String, Object> where = new LinkedHashMap<String, Object>(); 
			where.put("USERNAME", user.get("USERNAME"));
			rq.setWhere(where);
			
			
			Map<String, Object> updatedUser = queryService.update(rq, "USERS" );
			
			return updatedUser;
			
		} else {
			throw new UserDuplicateException(user.get("USERNAME").toString());
		}
	}
	
	
	public void forgotPassword(String email) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException{
		RequestQuery rq = new RequestQuery();
		Map<String, Object> user = new LinkedHashMap<>();
		user.put("USERNAME", email);
		if (!existUser(user)) {
			throw new UserNotFoudException(email);
		}
		user.put("TOKENACTIVATION",UUID.randomUUID().toString());
		rq.setParams(user);
		Map<String, Object> where = new LinkedHashMap<String, Object>(); 
		where.put("USERNAME", email);
		rq.setWhere(where);
		queryService.update(rq, "USERS");
		sendEmailForgotPassword(user);
	}
	
	public void sendEmailForgotPassword(Map<String, Object> user)throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		Map<String, Object> data = new HashMap<>();
		data.put("userName", user.get("USERNAME"));
		data.put("domain", config.getUrlEmailRestorePassword());
		data.put("token", user.get("TOKENACTIVATION"));
		data.put("logo", config.getUrlLogo());
        Template  template = configuration.getTemplate(config.getEmailRestorePasswordTemplate());
        String readyParsedTemplate = FreeMarkerTemplateUtils.processTemplateIntoString(template,data);
		smtpMailSender.send(user.get("USERNAME").toString(), "Cambio de Contraseña de Cuenta", readyParsedTemplate);
	}
	

	public Boolean existUser(Map<String,Object> user) {
		String query = "SELECT USERNAME  FROM USERS WHERE USERNAME = :username";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("username", user.get("USERNAME"));
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}

	public Boolean existToken(String token) {
		String query = "SELECT USERNAME  FROM USERS WHERE TOKENACTIVATION = :token";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("token", token);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}

	public void activateAccount(String token, String password) {
		BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

		if (!existToken(token)) {
			throw new TokenExpiredException();
		}
		RequestQuery rq = new RequestQuery();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("PASSWORD", bcryptEncoder.encode(password));
		params.put("ACTIVE", true);
		params.put("TOKENACTIVATION", null);
		
		Map<String,Object>	whereRequestQuery = new LinkedHashMap<String, Object>();
		whereRequestQuery.put("TOKENACTIVATION", token);
		rq.setWhere(whereRequestQuery);
		rq.setParams(params);
		queryService.update(rq, "USERS");
	}

	private void sendMailActivation(Map<String,Object> user, String pathTemplate) throws TemplateNotFoundException, 
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		Map<String, Object> data = new HashMap<>();
		data.put("userName", user.get("USERNAME"));
		data.put("name", user.get("FULLNAME"));
		data.put("appName", config.getAppName());
		data.put("domain", config.getUrlEmailActivation());
		data.put("token", user.get("TOKENACTIVATION"));
		data.put("logo", config.getUrlLogo());
		// rabbitMQSender.send(data);
		Template template = null;
		if(pathTemplate!="") {
			//template = configuration.getTemplate("/opt/files/templates/activation-account-ronda.html");
			template = configuration.getTemplate(config.getEmailActivationTemplate());
		}else {
			template = configuration.getTemplate(config.getEmailActivationTemplate());
		}
		
		
		String readyParsedTemplate = FreeMarkerTemplateUtils.processTemplateIntoString(template, data);
		smtpMailSender.send(user.get("USERNAME").toString(), "Activación de Cuenta Monitoreo " + config.getCompany(),
				readyParsedTemplate);
	}
	
	public List<Map<String, Object>> getUsers(){
		String query = "SELECT ID_USER \"ID_USER\", FULLNAME \"FULLNAME\", USERNAME \"USERNAME\", LDAP \"LDAP\", ACTIVE \"ACTIVE\" FROM USERS";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	public List<Map<String, Object>> getRoles(){
		String query = "SELECT ID_ROLE \"ID_ROLE\", NAME \"NAME\" FROM ROLE";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	
	public Map<String, Object> getRoleByName(String name){
		String query = "SELECT ID_ROLE \"ID_ROLE\", NAME \"NAME\" FROM ROLE WHERE NAME = :name";
		RequestQuery requestQuery = new RequestQuery();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("name", name);
		requestQuery.setParams(params);
		 List<Map<String, Object>> response = queryService.getResultQuery(query, requestQuery);
		if(response.size()>0) {
			return response.get(0);
		}
		
		return null;
	}
	
	public List<Map<String, Object>> getRoleByUsername(String username){
		String query = "  SELECT  r.id_role id, r.name FROM USERS U "
				+ "  INNER JOIN role_users RU ON RU.user_id = U.id_user "
				+ "  INNER JOIN ROLE R ON  R.id_role = RU.role_id "
				+ "  WHERE username =  :username";
		RequestQuery requestQuery = new RequestQuery();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("username", username);
		requestQuery.setParams(params);
		List<Map<String, Object>> response = queryService.getResultQuery(query, requestQuery);
		return response;
	}
	public List<Map<String, Object>> getRolesUser(String userId){
		String query = "SELECT ID_USER \"ID_USER\", ID_ROLE \"ID_ROLE\", NAME \"ROLNAME\", FULLNAME \"FULLNAME\", USERNAME \"EMAIL\"   "
				+ " FROM ROLE INNER JOIN ROLE_USERS  ON ( ROLE_ID= ID_ROLE ) " 
				+ " INNER JOIN USERS ON  ( USER_ID=ID_USER  ) "
				+ " WHERE USER_ID =:userId   "
				+ " ORDER BY NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("userId",  new BigInteger(userId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	
	
	public List<Map<String, Object>> getRolesUserChecked(String userId) {
		String query = "SELECT :userId \"ID_USER\", (CASE WHEN EM.USER_ID IS NULL THEN 0 ELSE 1 END)  \"CHECKED\","
				+ " ID_ROLE \"ID_ROLE\", NAME \"ROLNAME\""
				+ " , (SELECT USERNAME FROM USERS WHERE ID_USER=:userId ) \"EMAIL\" "
				+ " FROM ROLE LEFT JOIN (SELECT ROLE_ID, USER_ID FROM ROLE_USERS WHERE USER_ID =:userId ) EM  ON ( ROLE_ID= ID_ROLE )  "
				+ " ORDER BY NAME ";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("userId", new BigInteger(userId));

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	
	public List<Map<String, Object>> getRolesUserCheckedByUsuarioOperadoras(String userId, String usernameLogged) {
		String query = "WITH CTE AS (SELECT id_role, name, "
				+ "(CASE WHEN RU.USER_ID = :userId THEN 1 ELSE 0 END) AS CHECKED   FROM role R   "
				+ "INNER JOIN role_users RU ON RU.role_id = R.id_role   "
				+ "INNER JOIN users U ON RU.user_id = U.id_user   "
				+ "WHERE username = :usernam OR user_id = user_id ) "
				+ "SELECT id_role, name, CHECKED, rn FROM "
				+ "(   SELECT id_role, name, CHECKED,  ROW_NUMBER() "
				+ "OVER (PARTITION BY id_role, name ORDER BY CHECKED DESC) AS rn   FROM CTE ) AS Subquery "
				+ "WHERE rn = 1";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("userId",  new BigInteger(userId));
		q.setParameter("username", usernameLogged);

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		return hibernateQuery.list();

	}
	
}
