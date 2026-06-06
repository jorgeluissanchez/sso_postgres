package com.co.lowcode.security.gateway;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

public class MyZuulFilter extends ZuulFilter {

    private static Logger LOGGER = LoggerFactory.getLogger(MyZuulFilter.class);

    private static final String FILTERTYPE = "pre";

    private static final Integer FILTERORDER = 1;

    public MyZuulFilter(){
        //For Spring
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
    
    private String getRequestBody(RequestContext context) {
	    try (InputStream in = (InputStream) context.get("requestEntity")) {

	        String bodyText;
	        if (in == null) {
	            bodyText = StreamUtils.copyToString(context.getRequest().getInputStream(), Charset.forName("UTF-8"));
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
        LOGGER.info("{} petición a {}", request.getHeader("Authorization"));
        LOGGER.info("{} petición a {}", request.getMethod(), request.getRequestURL().toString());
        LOGGER.info("body {}",getRequestBody(ctx));
        request.getRemoteUser();
        return null;
    }

}