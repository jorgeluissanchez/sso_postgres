package com.co.lowcode.security.gateway;

import java.util.HashSet;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.co.lowcode.lineabase.model.MicroService;
import com.co.lowcode.lineabase.repository.MicroservicioRepository;


@Service
public class ZuulDynamicRoutingService {

	private static final Logger logger = LoggerFactory.getLogger(ZuulDynamicRoutingService.class);
	private static final String HTTP_PROTOCOL = "http://";

	private final ZuulProperties zuulProperties;
	private final ZuulHandlerMapping zuulHandlerMapping;

	private final MicroservicioRepository dynamicRouteRedisRepository;
	
	@Autowired
	public ZuulDynamicRoutingService(final ZuulProperties zuulProperties, final ZuulHandlerMapping zuulHandlerMapping,
			final MicroservicioRepository dynamicRouteRedisRepository) {
		this.zuulProperties = zuulProperties;
		this.zuulHandlerMapping = zuulHandlerMapping;
		this.dynamicRouteRedisRepository = dynamicRouteRedisRepository;
	}
	
	/**
	 * Load all routes from microservices  to restore the existing routes while restarting the zuul server
	 */
	@PostConstruct
	public void initialize() {
		try {
			dynamicRouteRedisRepository.findAll().forEach(dynamicRoute -> {
				addDynamicRouteInZuul(dynamicRoute);
			});
			zuulHandlerMapping.setDirty(true);
		} catch (Exception e) {
			logger.error("Exception in loading any previous route while restarting zuul routes.", e);
	    }
	}

	public DynamicRouteResponse addDynamicRoute(MicroService dynamicRoute) {
		logger.debug("request received in service to add {}", dynamicRoute);
		addDynamicRouteInZuul(dynamicRoute);
		
		logger.debug("going to add in cache {}", dynamicRoute);
		addToCache(dynamicRoute);
		logger.debug("added in cache {}", dynamicRoute);
		zuulHandlerMapping.setDirty(true);

		DynamicRouteResponse dynamicRouteResponse = new DynamicRouteResponse();
		logger.debug("response sent {}", dynamicRouteResponse);
		return dynamicRouteResponse;
	}

	public Boolean removeDynamicRoute(final String requestURIUniqueKey) {
		MicroService dynamicRoute = new MicroService();
		//Removal from redis will be done from unique key. No need for other params. So create object
		//with just unique key
		dynamicRoute.setServiceId(requestURIUniqueKey);
		if (zuulProperties.getRoutes().containsKey(requestURIUniqueKey)) {
			ZuulRoute zuulRoute = zuulProperties.getRoutes().remove(requestURIUniqueKey);
			logger.debug("removed the zuul route {}", zuulRoute);
			//Removal from redis will be done from unique key. No need for other params
			removeFromCache(dynamicRoute);
			zuulHandlerMapping.setDirty(true);
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	private void addDynamicRouteInZuul(MicroService dynamicRoute) {
		
		String url = null;
		String serviceId = null;
		if(dynamicRoute.getTargetURLHost() !=null) {
			url = createTargetURL(dynamicRoute);
		}
		if(dynamicRoute.getServiceId() != null) {
			serviceId = dynamicRoute.getServiceId();
		}
		zuulProperties.getRoutes().put(dynamicRoute.getId().toString(),
				new ZuulRoute(dynamicRoute.getId().toString(), dynamicRoute.getRequestURI() + "/**",
						serviceId, url, true, false, new HashSet<>()));
		System.out.println("PRueba");
	}

	private String createTargetURL(MicroService dynamicRoute) {
		StringBuilder sb = new StringBuilder(HTTP_PROTOCOL);
		sb.append(dynamicRoute.getTargetURLHost()).append(":").append(dynamicRoute.getTargetURLPort());
		if (StringUtils.isEmpty(dynamicRoute.getTargetURIPath())) {
			sb.append("");
		} else {
			sb.append(dynamicRoute.getTargetURIPath());
		}
		String url = sb.toString();
		return url;
	}
	private void addToCache(final MicroService dynamicRoute) {
		MicroService dynamicRouteSaved = dynamicRouteRedisRepository.save(dynamicRoute);
		logger.debug("Added in cache {}", dynamicRouteSaved);
	}
	
	private void removeFromCache(final MicroService dynamicRoute) {
		logger.debug("removing the dynamic route {}", dynamicRoute);
		//Removal from redis will be done from unique key. No need for other params
		dynamicRouteRedisRepository.delete(dynamicRoute);
	}

}
