package com.co.lowcode.security.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.co.lowcode.service.RedisService;


public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationConfig config;
    
    private RedisService redisService;

    public JwtTokenAuthenticationFilter(JwtAuthenticationConfig config, RedisService redisService) {
        this.config = config;
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse rsp, FilterChain filterChain)
            throws ServletException, IOException {
    	
    	
    	
    	
        String token = req.getHeader(config.getHeader());
        if (token != null && token.startsWith(config.getPrefix() + " ")) {
            token = token.replace(config.getPrefix() + " ", "");
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(config.getSecret().getBytes())
                        .parseClaimsJws(token)
                        .getBody();
                String username = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> authorities = claims.get("authorities", List.class);
                
                if (username != null) {
                	
                	ObjectMapper mapper = new ObjectMapper();
                	mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            	    CollectionType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Map.class);
            	    List<Map<String, Object>> endpoints = null ;
                	if( claims.get("uuid") != null) {
                		String uuid = claims.get("uuid").toString();
                		String json =  (String) redisService.getValue(String.format("%s:%s", username, uuid));
                		endpoints = mapper.readValue(json, listType);
                	}

                	List<String> jsonString = claims.get("endpoints", List.class);
                	 
                	
                	if(jsonString != null) {
                		mapper = new ObjectMapper();
                    	endpoints = mapper.convertValue(jsonString, new TypeReference<List<Map<String, Object>>>() { });
                	}
                	
                	
                	boolean sw = false;
                	for(Map<String, Object> e : endpoints) {
                		if(e.get("numberParams") == null) {
                			e.put("numberParams", 0);
                		}
                		if(req.getMethod().equals(e.get("method")) && 
                				req.getRequestURI().replaceAll("//", "/").equals(
                						(e.get("requestURI") + "" +  e.get("path"))) &&
                				req.getParameterMap().size() == (Integer)e.get("numberParams")){
                			sw = true;
                			break;
                		}
                	}
                	if(sw) {
                		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
                                authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                	}
                    
                }
              
            } catch (Exception ignore) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(req, rsp);
    }
}
