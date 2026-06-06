package org.minminas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.minminas.util.ConfigProperties;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;

public class Ldap {
	private static Ldap INSTANCE = new Ldap();

	private Ldap() {
	}

	public static enum Response {
		EXIST, NO_EXIST, VALID, PASSWORD_FAULT, AUTHENTICATION_FAULT, FAULT;

		private Response() {
		}
	}
	
	public static Ldap getInstance() {
		return INSTANCE;
	}

	public Response getAuthentication(String userName, String password) {
		String base = "";
		String directoryString = "";
		Response response = Response.AUTHENTICATION_FAULT;
		try {
			base = ConfigProperties.getProperties().getString("ldap.base.schema");
			
			directoryString = ConfigProperties.getProperties().getString("ldap.directoryString");
			LdapContextSource ctxSrc = new LdapContextSource();
			ctxSrc.setUrl(ConfigProperties.getProperties().getString("ldap.url"));
			ctxSrc.setBase(ConfigProperties.getProperties().getString("ldap.base"));
			ctxSrc.setUserDn(ConfigProperties.getProperties().getString("ldap.userDn"));
			ctxSrc.setPassword(ConfigProperties.getProperties().getString("ldap.password"));
			
			ctxSrc.setPooled(false);
			ctxSrc.afterPropertiesSet();

			LdapTemplate lt = new LdapTemplate(ctxSrc);
			lt.setIgnorePartialResultException(true);


			Filter filter1 = new EqualsFilter(directoryString, userName);

			List<String> list = lt.search(base, filter1.toString(), new ContactAttributeMapperJSON());

			boolean authenticated = lt.authenticate(base, filter1.toString(), password);

			if (list.isEmpty()) {
				response = Response.NO_EXIST;
			} else if ((authenticated == true) && (password.isEmpty())) {
				response = Response.EXIST;
			} else if (authenticated == true) {
				response = Response.VALID;
			} else {
				response = Response.PASSWORD_FAULT;
			}
		} catch (AuthenticationException ae) {
			response = Response.AUTHENTICATION_FAULT;
		} catch (Exception e) {
			response = Response.FAULT;
		}

		return response;
	}
	
	public List<Map<String,Object>>  getAllUsers() throws Exception {
		String base = ConfigProperties.getProperties().getString("ldap.base.schema");
		LdapContextSource ctxSrc = new LdapContextSource();
		ctxSrc.setUrl(ConfigProperties.getProperties().getString("ldap.url"));
		ctxSrc.setBase(ConfigProperties.getProperties().getString("ldap.base"));
		ctxSrc.setUserDn(ConfigProperties.getProperties().getString("ldap.userDn"));
		ctxSrc.setPassword(ConfigProperties.getProperties().getString("ldap.password"));
		
		ctxSrc.setPooled(false);
		ctxSrc.afterPropertiesSet();

		LdapTemplate lt = new LdapTemplate(ctxSrc);
		SearchControls controls = new SearchControls();
		controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		controls.setCountLimit(10);
	//	List<String> result = lt.search(base, "(objectclass=person)", controls, new ContactAttributeMapperJSON());
		
		@SuppressWarnings("unchecked")
		List<Map<String,Object>> response = lt.search(base, "(objectclass=person)", controls,  new AttributesMapper() {
			@Override
			public Object mapFromAttributes(Attributes attributes) throws NamingException {
				Map<String,Object> response = new HashMap<>();
				try {
				response.put("username", attributes.get("sAMAccountName").get().toString());
				response.put("name", attributes.get("displayname").get().toString());
				
				}catch(Exception e){
					
				}
				return response;
			}
		});
		
		
		/*List<Map<String,Object>> response = new ArrayList<Map<String,Object>>();
		for(String r : result) {
			JSONObject jsonObjectData = new JSONObject(r);
			response.add(JsonUtils.jsonToMap(jsonObjectData));
		}*/
		return response;
	}
}
