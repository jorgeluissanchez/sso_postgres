package com.co.lowcode.security.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.co.lowcode.lineabase.model.Role;
import com.co.lowcode.lineabase.oauth.model.LoginResponse;
import com.co.lowcode.lineabase.oauth.model.RouteResponse;
import com.co.lowcode.lineabase.oauth.model.UserAuth;
import com.co.lowcode.lineabase.oauth.service.RoleService;
import com.co.lowcode.lineabase.oauth.service.UserServiceI;
import com.co.lowcode.service.RedisService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Authenticate the request to url /login by POST with json body '{ username, password }'.
 * If successful, response the client with header 'Authorization: Bearer jwt-token'.
 *
 */
public class JwtUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final JwtAuthenticationConfig config;
    private final ObjectMapper mapper;
    private UserAuth userAuth;

    private RedisService redisService;
    
    private RoleService roleService;
    
    private UserServiceI userService;
    
    public JwtUsernamePasswordAuthenticationFilter(JwtAuthenticationConfig config,
    		AuthenticationManager authManager,RoleService roleService,
    		UserServiceI userService, RedisService redisService) {
        super(new AntPathRequestMatcher(config.getUrl(), "POST"));
        setAuthenticationManager(authManager);
        this.config = config;
        this.mapper = new ObjectMapper();
        this.roleService = roleService;
        this.userService = userService;
        this.redisService = redisService;
    }
    

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse rsp)
            throws AuthenticationException, IOException {
    	userAuth = mapper.readValue(req.getInputStream(), UserAuth.class);
        return getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(
        		userAuth,  userAuth.getPassword(), Collections.emptyList()
        ));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse rsp, FilterChain chain,
                                            Authentication auth) throws JsonParseException, JsonMappingException, IOException {
    	

    	List<String> authorites = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
    	List<Map<String, Object>>  endpoints  = new ArrayList<>();
    	/*for(String role : authorites) {
    		List<Map<String, Object>>  e = roleService.getEndpointByRoleId(role);
    		endpoints.addAll(e);
    	}*/
    	
		List<Map<String, Object>> endpointsResult = roleService.getEndpointByRoleId(authorites);
		endpoints.addAll(endpointsResult);
    	
    	
    	
    	
    	String uuid = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String token = Jwts.builder()
                .setSubject(auth.getName())
                .claim("authorities", authorites)
              //  .claim("endpoints",endpoints)
                .claim("uuid", uuid)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(config.getExpiration())))
                .signWith(SignatureAlgorithm.HS256, config.getSecret().getBytes())
                
                .compact();
        //rsp.addHeader(config.getHeader(), config.getPrefix() + " " + token);
        
        
        LoginResponse loginResponse = new LoginResponse();
        
        
        List<RouteResponse> routes = userService.findOpcionByUsernameAndApp(auth.getName(),userAuth.getApp_name());
        loginResponse.setRoutes(routes);
        
      
        String refreshToken = UUID.randomUUID().toString();
        loginResponse.setRefreshToken(refreshToken);
       
        userService.saveRefreshToken(auth.getName(),refreshToken);
        
        
        loginResponse.setToken(token);
        
       
        try {
        	redisService.setValue(String.format("%s:%s", auth.getName().toLowerCase(), uuid),
        						  endpoints, TimeUnit.SECONDS, 3600L, true);
        }catch(Exception e) {
        	throw new IOException("ERROR RD: Comuniquese con el administrador");
        }

        
        PrintWriter out;
		try {
			out = rsp.getWriter();
			rsp.setContentType("application/json");
		    rsp.setCharacterEncoding("UTF-8");
		    ObjectMapper mapperObj = new ObjectMapper();
		    String JSON = mapperObj.writeValueAsString(loginResponse);
		    out.print(JSON);
		    out.flush();   
		} catch (IOException e) {
			e.printStackTrace();
		}
      
        
    }
}
