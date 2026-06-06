package com.co.lowcode.sso.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:general.properties")
public class GeneralConfig {


	@Value("${database_engine}")
	private String databaseEngine;
	
	@Value("${files_path}")
	private String filesPath;
	
	@Value("${email.activation.template}")
	private String emailActivationTemplate;
	
	@Value("${email.activation.url}")
	private String urlEmailActivation;
	
	@Value("${email.logo.url}")
	private String urlLogo;
	
	@Value("${email.company}")
	private String company;

	@Value("${email.app.name}")
	private String appName;
	
	@Value("${database.context.name}")
	private String contextName;
	
	
	@Value("${email.restore.password.template}")
	private String emailRestorePasswordTemplate;
	
	@Value("${email.restore.password.url}")
	private String urlEmailRestorePassword;

	public String getDatabaseEngine() {
		return databaseEngine;
	}

	public void setDatabaseEngine(String databaseEngine) {
		this.databaseEngine = databaseEngine;
	}

	public String getFilesPath() {
		return filesPath;
	}

	public void setFilesPath(String filesPath) {
		this.filesPath = filesPath;
	}

	public String getEmailActivationTemplate() {
		return emailActivationTemplate;
	}

	public void setEmailActivationTemplate(String emailActivationTemplate) {
		this.emailActivationTemplate = emailActivationTemplate;
	}

	public String getUrlEmailActivation() {
		return urlEmailActivation;
	}

	public void setUrlEmailActivation(String urlEmailActivation) {
		this.urlEmailActivation = urlEmailActivation;
	}

	public String getUrlLogo() {
		return urlLogo;
	}

	public void setUrlLogo(String urlLogo) {
		this.urlLogo = urlLogo;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getEmailRestorePasswordTemplate() {
		return emailRestorePasswordTemplate;
	}

	public void setEmailRestorePasswordTemplate(String emailRestorePasswordTemplate) {
		this.emailRestorePasswordTemplate = emailRestorePasswordTemplate;
	}

	public String getUrlEmailRestorePassword() {
		return urlEmailRestorePassword;
	}

	public void setUrlEmailRestorePassword(String urlEmailRestorePassword) {
		this.urlEmailRestorePassword = urlEmailRestorePassword;
	}

	public String getContextName() {
		return contextName;
	}

	public void setContextName(String contextName) {
		this.contextName = contextName;
	}
	
	
}
