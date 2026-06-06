package com.co.lowcode.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:general.properties")
public class GeneralConfig {

	@Value("${backend.url}")
	private String backenUrl;
	
	@Value("${database_engine}")
	private String databaseEngine;
	
	@Value("${database.context.name}")
	private String contextName;

	public String getDatabaseEngine() {
		return databaseEngine;
	}

	public void setDatabaseEngine(String databaseEngine) {
		this.databaseEngine = databaseEngine;
	}


	public String getBackenUrl() {
		return backenUrl;
	}

	public void setBackenUrl(String backenUrl) {
		this.backenUrl = backenUrl;
	}

	public String getContextName() {
		return contextName;
	}

	public void setContextName(String contextName) {
		this.contextName = contextName;
	}
	
	
}