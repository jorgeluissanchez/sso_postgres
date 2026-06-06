package com.co.lowcode.lineabase.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "external_auth")
public class ExternalAuth {
	
	
	private Long idExternalAuth;
	private String name;
	
	private String username;
	private String password;
	private String splitCharacter;
	private String type; //Basic, BearerToken, NoAuth, ApyKey
	private String token;
	private String urlAuth;
	private String method;
	private String keyword;
	
	private List<ExternalEndpoint> externalEndpoint;
	


	@Id
	@Column(name = "id_external_auth")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getIdExternalAuth() {
		return idExternalAuth;
	}
	public void setIdExternalAuth(Long idExternalAuth) {
		this.idExternalAuth = idExternalAuth;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getSplitCharacter() {
		return splitCharacter;
	}
	public void setSplitCharacter(String splitCharacter) {
		this.splitCharacter = splitCharacter;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getUrlAuth() {
		return urlAuth;
	}
	public void setUrlAuth(String urlAuth) {
		this.urlAuth = urlAuth;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "externalAuth")
	public List<ExternalEndpoint> getExternalEndpoint() {
		return externalEndpoint;
	}
	public void setExternalEndpoint(List<ExternalEndpoint> externalEndpoint) {
		this.externalEndpoint = externalEndpoint;
	}
	@Column(nullable=false)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
	
}