package com.co.lowcode.lineabase.oauth.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.co.lowcode.lineabase.model.MicroService;
import com.co.lowcode.lineabase.model.Role;
import com.co.lowcode.lineabase.model.Route;
import com.co.lowcode.lineabase.repository.RoleRepository;

@Service
public class RoleService {
	@Autowired
	RoleRepository roleRepository;
	
	@PersistenceContext
	private EntityManager entityManager;

	
	public Iterable<Role> findAll() {
		return roleRepository.findAll();
	}
	
	public Role save(Role role){
		Role roleCreated = roleRepository.save(role);
		return roleCreated;
	}
	
	public Role getByName(String name){
		return roleRepository.findByName(name);
	}
	

	public Set<Role> getRolesByUsername(String username){
		Query q = entityManager.createNativeQuery("select r.id_role, r.name from role r "
				+ " inner join role_users ru on r.id_role = ru.role_id"
				+ " inner join users u on u.id_user = ru.user_id where username = :username");
		q.setParameter("username", username);
		List<Object[]> result =  q.getResultList();
		Set<Role> roles  = new HashSet<Role>(); 
		for(Object[] o : result) {
			Role response = new Role();
			response.setId(((BigInteger)o[0]).longValue());
			response.setName((String)o[1]);
			//response.setRoutes(getRouteByRoleId(response.getId()));
			
			roles.add(response);
		}
		return roles;
	}
	
	public Set<Route> getRouteByRoleId(String idRole){
		Query q = entityManager.createNativeQuery("select r.id_route, r.name, r.path, r.icon, r.menuOrder, r.idParent from route r "
				+ " inner join role_route rr on r.id_route = rr.route_id"
				+ " inner join role ro on ro.id_role = rr.role_id where ro.id_role = :idRole");
		q.setParameter("idRole", idRole);
		List<Object[]> result =  q.getResultList();
		Set<Route> routes  = new HashSet<Route>(); 
		for(Object[] o : result) {
			Route response = new Route();
			response.setIdRoute(((BigInteger)o[0]).longValue());
			response.setName((String)o[1]);
			response.setPath((String)o[2]);
			//response.setIcon((String)o[3]);
			//response.setMenuOrder((Integer)o[4]);
			response.setIdParent(o[5]!=null?((BigInteger)o[5]).longValue():null);
			routes.add(response);
		}
		return routes;
	}
	
	public List<Map<String, Object>> getEndpointByRoleId(String name){
		Query q = entityManager.createNativeQuery("select e.id_endpoint, e.method, e.numberParams, e.path, m.requestURI, m.serviceId "
				+ " from endpoint e "
				+ " inner join role_endpoint re on e.id_endpoint = re.endpoint_id"
				+ " inner join role ro on ro.id_role = re.role_id "
				+ " inner join endpoint_microservice em on em.endpoint_id = e.id_endpoint "
				+ " inner join microservice m on m.id_microservice = em.microservice_id "
				+ "where ro.name = :name");
		q.setParameter("name", name);
		List<Object[]> result =  q.getResultList();
		List<Map<String, Object>> res = new ArrayList<>();
		for(Object[] o : result) {
			Map<String, Object> response = new HashMap<String, Object>();
			response.put("idEndpoint", ((BigInteger)o[0]).longValue());
			response.put("method", (String)o[1]);
			response.put("numberParams", (Integer)o[2]);
			response.put("path",(String)o[3]);
			response.put("requestURI",(String)o[4]);
			response.put("serviceId",(String)o[5]);
			res.add(response);
		}
		return res;
	}
	
	public List<Map<String, Object>> getEndpointByRoleId(List<String> names){
		Query q = entityManager.createNativeQuery("select e.id_endpoint,e.method, e.numberParams, e.path, m.requestURI, m.serviceId "
				+ " from endpoint e "
				+ " inner join role_endpoint re on e.id_endpoint = re.endpoint_id"
				+ " inner join role ro on ro.id_role = re.role_id "
				+ " inner join endpoint_microservice em on em.endpoint_id = e.id_endpoint "
				+ " inner join microservice m on m.id_microservice = em.microservice_id "
				+ "where ro.name IN :names");
		q.setParameter("names", names);
		List<Object[]> result =  q.getResultList();
		List<Map<String, Object>> res = new ArrayList<>();
		for(Object[] o : result) {
			Map<String, Object> response = new HashMap<String, Object>();
			response.put("idEndpoint", ((BigInteger)o[0]).longValue());
			response.put("method", (String)o[1]);
			response.put("numberParams", (Integer)o[2]);
			response.put("path",(String)o[3]);
			response.put("requestURI",(String)o[4]);
			response.put("serviceId",(String)o[5]);
			res.add(response);
		}
		return res;
	}
	
	
	public Set<MicroService> getMicroserviceByEndpointId(Long idEndpoint){
		Query q = entityManager.createNativeQuery("select m.id_microservice, m.requestURI, m.serviceId from microservice m "
				+ " inner join endpoint_microservice em on m.id_microservice = em.microservice_id"
				+ " inner join endpoint e on e.id_endpoint = em.endpoint_id where e.id_endpoint = :idEndpoint");
		q.setParameter("idEndpoint", idEndpoint);
		List<Object[]> result =  q.getResultList();
		Set<MicroService> microservices  = new HashSet<MicroService>(); 
		for(Object[] o : result) {
			MicroService response = new MicroService();
			response.setId(((BigInteger)o[0]).longValue());
			response.setRequestURI((String)o[1]);
			response.setServiceId((String)o[2]);
			microservices.add(response);
		}
		return microservices;
	}
}