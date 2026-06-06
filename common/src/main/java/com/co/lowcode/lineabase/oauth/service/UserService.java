package com.co.lowcode.lineabase.oauth.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.co.lowcode.lineabase.model.Endpoint;
import com.co.lowcode.lineabase.model.Role;
import com.co.lowcode.lineabase.model.Route;
import com.co.lowcode.lineabase.model.User;
import com.co.lowcode.lineabase.oauth.model.LoginResponse;
import com.co.lowcode.lineabase.oauth.model.RouteResponse;
import com.co.lowcode.lineabase.oauth.model.UserRepositoryUserDetails;
import com.co.lowcode.lineabase.repository.UserRepository;
import com.co.lowcode.security.common.JwtAuthenticationConfig;
import com.co.lowcode.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

@Service
public class UserService implements UserServiceI, UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	JwtAuthenticationConfig config;

	@Autowired
	RedisService redisService;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	RoleService roleService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// User user = userRepository.findByUsername(username);
		User user = getUserByUserNameLocal(username);
		if (user == null) {
			throw new UsernameNotFoundException(String.format("El usuario %s no existe", username));
		}
		user.setRoles(roleService.getRolesByUsername(username));

		return new UserRepositoryUserDetails(user);
	}
	
	@Override
	public UserDetails loadUserByUsername(User user) throws UsernameNotFoundException {
		user.setRoles(roleService.getRolesByUsername(user.getUsername()));
		return new UserRepositoryUserDetails(user);
	}

	@Override
	public List<Map<String, Object>> getUsersSSO() {
		Query q = entityManager.createNativeQuery("select username" + "      ,fullName from users where active = 1");

		List<Object[]> result = q.getResultList();
		List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
		for (Object[] o : result) {
			Map<String, Object> r = new HashMap<>();
			r.put("username", o[0]);
			r.put("name", o[1]);
			response.add(r);
		}
		return response;

	}

	@Override
	public User getUserByUserName(String username) {
		return userRepository.findByUsername(username);
	}

	@Override
	public User getUserByUserNameLocal(String username) {
		Query q = entityManager.createNativeQuery("select id_user,active" + "      ,fullName" + "      ,ldap"
				+ "      ,password" + "      ,refreshToken" + "      ,tokenActivation" + "      ,tokenRestore"
				+ "      ,username" + "      ,apiToken from users where username = :username");
		q.setParameter("username", username);
		List<Object[]> result = q.getResultList();
		User response = null;
		for (Object[] o : result) {
			response = new User();
			response.setId((((BigInteger) o[0])).longValue());
			response.setActive((Boolean) o[1]);
			response.setFullName((String) o[2]);
			response.setLdap((Boolean) o[3]);
			response.setPassword((String) o[4]);
			response.setRefreshToken((String) o[5]);
			response.setTokenActivation((String) o[6]);
			response.setTokenRestore((String) o[7]);
			response.setUsername((String) o[8]);
			response.setApiToken((String) o[9]);
		}
		return response;
	}

	@Override
	public Set<Role> findRoleByUsername(String username) {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			return null;
		}
		Set<Role> r = user.getRoles();
		for (Role role : r) {
			Set<Route> opcion = role.getRoutes();
			for (Route o : opcion) {
				o.setRoles(null);
			}
		}
		return r;
	}

	@Override
	public List<RouteResponse> findOpcionByUsernameAndApp(String username, String app_name) {

		Query q = entityManager.createNativeQuery("select distinct o.id_route, o.idParent, o.name, "
				+ "									o.path, o.type, o.icon, o.menuOrder "
				+ "						from users u "
				+ "				        inner join role_users ur on u.id_user = ur.user_id "
				+ "				        inner join role r2 on ur.role_id = r2.id_role "
				+ "				        inner join role_route ro on r2.id_role = ro.role_id "
				+ "				        inner join route o on ro.route_id = o.id_route "
				+ "				        inner join app_route ao on o.id_route = ao.route_id "
				+ "				        inner join app a on ao.app_id = a.id_app "
				+ "				 where username = :username and  a.name = :app_name order by o.id_route asc");

		q.setParameter("username", username);
		q.setParameter("app_name", app_name);
		List<Object[]> result = q.getResultList();
		List<RouteResponse> opciones = new ArrayList<>();
		for (Object[] o : result) {
			RouteResponse opcion = new RouteResponse();
			if (o[0] != null) {
				opcion.setIdRoute((((BigInteger) o[0])).longValue());
			}
			if (o[1] != null) {
				opcion.setIdParent((((BigInteger) o[1])).longValue());
			}

			opcion.setNameRoute((String) o[2]);
			opcion.setPath((String) o[3]);
			opcion.setType((String) o[4]);

			opcion.setIcon(o[5] != null ? (String) o[5] : null);
			opcion.setMenuOrder(o[6] != null ? (Integer) o[6] : null);

			opciones.add(opcion);
		}

		return opciones;
	}

	@Override
	public List<RouteResponse> findOpcionByApiTokenAndApp(String apiToken, String app_name) {

		Query q = entityManager
				.createNativeQuery("select distinct o.name, o.path, o.type, c.uuid as component_id from users u "
						+ "				        inner join role_users ur on u.id_user = ur.user_id "
						+ "				        inner join role r2 on ur.role_id = r2.id_role "
						+ "				        inner join role_route ro on r2.id_role = ro.role_id "
						+ "				        inner join route o on ro.route_id = o.id_route "
						+ "				        inner join app_route ao on o.id_route = ao.route_id "
						+ "				        inner join app a on ao.app_id = a.id_app "
						+ "                     inner join component c on c.id_component = o.component_id_component "
						+ "				 where username = :apiToken and  a.name = :app_name");

		q.setParameter("apiToken", apiToken);
		q.setParameter("app_name", app_name);
		List<Object[]> result = q.getResultList();
		List<RouteResponse> opciones = new ArrayList<>();
		for (Object[] o : result) {
			RouteResponse opcion = new RouteResponse();
			/*
			 * if(o[0]!=null) { opcion.setIdRoute((((BigDecimal)o[0])).longValue()); }
			 * if(o[1]!=null) { opcion.setIdParent((((BigDecimal)o[1])).longValue()); }
			 */
			opcion.setNameRoute((String) o[0]);
			opcion.setPath((String) o[1]);
			opcion.setType((String) o[2]);
			opcion.setComponentId((String) o[3]);
			opciones.add(opcion);
		}

		return opciones;
	}

	@Override
	@Transactional
	public void saveRefreshToken(String username, String refreshToken) {

		Query q = entityManager
				.createNativeQuery("update users set refreshToken = :refreshToken " + "where username = :username");

		q.setParameter("refreshToken", refreshToken);
		q.setParameter("username", username);
		q.executeUpdate();

	}

	@Override
	@Transactional
	public LoginResponse getToken(HttpServletRequest request, String refreshToken, String appName) throws Exception {
		String tokenReq = request.getHeader("authorization").substring(7);
		Claims claims = Jwts.parser().setSigningKey(config.getSecret().getBytes()).parseClaimsJws(tokenReq).getBody();
		String username = claims.getSubject();
		List<String> authorites = new ArrayList<>();

		User user = getUserByUserNameLocal(username);
		if (user.getRefreshToken().equals(refreshToken)) {
			LoginResponse loginResponse = new LoginResponse();
			List<RouteResponse> routes = findOpcionByUsernameAndApp(username, appName);
			loginResponse.setRoutes(routes);
			String refreshTokenGen = UUID.randomUUID().toString();
			loginResponse.setRefreshToken(refreshTokenGen);

			saveRefreshToken(username, refreshTokenGen);

			List<Map<String, Object>> endpoints = new ArrayList<>();

			Set<Role> roles = roleService.getRolesByUsername(username);
			List<String> rolesName =  new ArrayList<>();
			for (Role r : roles) {
				authorites.add(r.getAuthority());
				rolesName.add(r.getName());
			}
			
			List<Map<String, Object>> endpointsResult = roleService.getEndpointByRoleId(rolesName);
			endpoints.addAll(endpointsResult);

			String uuid = UUID.randomUUID().toString();
			Instant now = Instant.now();
			String token = Jwts.builder().setSubject(username).claim("authorities", authorites)
					// .claim("endpoints",endpoints)
					.claim("uuid", uuid).setIssuedAt(Date.from(now))
					.setExpiration(Date.from(now.plusSeconds(config.getExpiration())))
					.signWith(SignatureAlgorithm.HS256, config.getSecret().getBytes())

					.compact();
			loginResponse.setToken(token);

			try {
				redisService.setValue(String.format("%s:%s", user.getUsername().toLowerCase(), uuid), endpoints,
						TimeUnit.SECONDS, 3600L, true);

			} catch (Exception e) {
				throw new Exception("ERROR RD: Comuniquese con el administrador");
			}

			return loginResponse;
		}
		return null;
	}

	@Override
	public LoginResponse getApiToken(String apiToken, String appName) throws IOException {

		List<String> authorites = new ArrayList<>();

		User user = userRepository.findByApiToken(apiToken);

		if (user != null) {
			LoginResponse loginResponse = new LoginResponse();
			List<RouteResponse> routes = findOpcionByApiTokenAndApp(apiToken, appName);
			loginResponse.setRoutes(routes);
			String refreshTokenGen = UUID.randomUUID().toString();
			loginResponse.setRefreshToken(refreshTokenGen);

			saveRefreshToken(user.getUsername(), refreshTokenGen);

			List<Endpoint> endpoints = new ArrayList<>();
			Set<Role> roles = user.getRoles();
			for (Role r : roles) {
				authorites.add(r.getAuthority());
				for (Endpoint e : r.getEndpoint()) {
					e.setId(null);
					endpoints.add(e);
				}
			}
			String uuid = UUID.randomUUID().toString();
			Instant now = Instant.now();
			String token = Jwts.builder().setSubject(user.getUsername()).claim("authorities", authorites)
					// .claim("endpoints",endpoints)
					.claim("uuid", uuid).setIssuedAt(Date.from(now))
					.setExpiration(Date.from(now.plusSeconds(config.getExpiration())))
					.signWith(SignatureAlgorithm.HS256, config.getSecret().getBytes())

					.compact();
			loginResponse.setToken(token);

			try {
				redisService.setValue(String.format("%s:%s", user.getUsername().toLowerCase(), uuid), endpoints,
						TimeUnit.SECONDS, 3600L, true);
			} catch (Exception e) {
				throw new IOException("ERROR RD: Comuniquese con el administrador");
			}

			return loginResponse;

		}

		return null;

	}

	@Override
	@Transactional
	public LoginResponse googleLogin(String idTokenString, String appName) {

		String CLIENT_ID = "300315544882-4q6geg34t8lii46t7tf89ci60vtfnoe8.apps.googleusercontent.com";
		NetHttpTransport transport = new NetHttpTransport();
		com.google.api.client.json.JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
				.setAudience(Collections.singletonList(CLIENT_ID)).build();

		GoogleIdToken idToken;
		try {
			idToken = verifier.verify(idTokenString);
			if (idToken != null) {
				Payload payload = idToken.getPayload();
				String userId = payload.getSubject();
				System.out.println("User ID: " + userId);
				String email = payload.getEmail();
				boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
				String name = (String) payload.get("name");
				String pictureUrl = (String) payload.get("picture");
				String locale = (String) payload.get("locale");
				String familyName = (String) payload.get("family_name");
				String givenName = (String) payload.get("given_name");
				System.out.println(name + " " + email);

				List<String> authorites = new ArrayList<>();

				List<Map<String, Object>> endpoints = new ArrayList<>();
				
				Set<Role> roles = roleService.getRolesByUsername(email);
				for (Role r : roles) {
					authorites.add(r.getAuthority());

					List<Map<String, Object>> e = roleService.getEndpointByRoleId(r.getName());
					endpoints.addAll(e);
				}
				
				
				String uuid = UUID.randomUUID().toString();
				Instant now = Instant.now();
				String token = Jwts.builder().setSubject(email).claim("authorities", authorites)
						// .claim("endpoints",endpoints)
						.claim("uuid", uuid).setIssuedAt(Date.from(now))
						.setExpiration(Date.from(now.plusSeconds(config.getExpiration())))
						.signWith(SignatureAlgorithm.HS256, config.getSecret().getBytes())

						.compact();
				// rsp.addHeader(config.getHeader(), config.getPrefix() + " " + token);

				LoginResponse loginResponse = new LoginResponse();

				List<RouteResponse> routes = findOpcionByUsernameAndApp(email, appName);
				loginResponse.setRoutes(routes);

				String refreshToken = UUID.randomUUID().toString();
				loginResponse.setRefreshToken(refreshToken);

				saveRefreshToken(email, refreshToken);

				loginResponse.setToken(token);

				try {
					redisService.setValue(String.format("%s:%s", email.toLowerCase(), uuid), endpoints,
							TimeUnit.SECONDS, 3600L, true);
				} catch (Exception e) {
					throw new IOException("ERROR RD: Comuniquese con el administrador");
				}

				return loginResponse;

			} else {
				System.out.println("Invalid ID token.");
			}
		} catch (

		GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

}
