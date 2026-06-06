package com.co.lowcode.security.gateway;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public class PostZuulFilter extends ZuulFilter {

	private static Logger LOGGER = LoggerFactory.getLogger(PostZuulFilter.class);

	private static final Integer FILTERORDER = 1;

	private static final String FILTERTYPE = "post";

	public PostZuulFilter() {
		// For Spring
	}

	@Override
	public String filterType() {
		return FILTERTYPE;
	}

	@Override
	public int filterOrder() {
		return FILTERORDER;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}
	
	

	private static final String[] IP_HEADER_NAMES = { "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
			"HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP",
			"HTTP_FORWARDED_FOR", "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR" };

	public static String getRemoteIP(HttpServletRequest request) {
		if (request == null) {
			return "0.0.0.0";
		}
		
		String ip = java.util.Arrays.asList(IP_HEADER_NAMES).stream().map(request::getHeader)
				.filter(h -> h != null && h.length() != 0 && !"unknown".equalsIgnoreCase(h)).map(h -> h.split(",")[0])
				.reduce("", (h1, h2) -> h1 + ":" + h2);
		return ip + request.getRemoteAddr();
	}
	
	private String getRequestBody(RequestContext context) {
	    try (InputStream in = (InputStream) context.get("requestEntity")) {

	        String bodyText;
	        if (in == null) {
	            bodyText = StreamUtils.copyToString(context.getRequest().getInputStream(), Charset.forName("ISO-8859-1"));
	        } else {
	            bodyText = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
	        }

	        return bodyText;
	    } catch (Exception e) {

	        return null;
	    }
	}

	@Override
	public Object run() {

		RequestContext ctx = RequestContext.getCurrentContext();
		final HttpServletRequest request = ctx.getRequest();
		//LOGGER.info("metodo {}", request.getMethod());
		//LOGGER.info("parametros {}", request.getParameterMap().toString());
		//LOGGER.info("user {}", request.getRemoteUser());
		//LOGGER.info("{} petición post a {} desde {} ip {} ", request.getMethod(), request.getRequestURL().toString(),
			//	request.getRemoteAddr(), getRemoteIP(request));
		
		
		//LOGGER.info("body {}",getRequestBody(ctx));
		
		return null;
	}

}