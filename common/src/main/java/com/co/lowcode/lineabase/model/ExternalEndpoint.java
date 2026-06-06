package com.co.lowcode.lineabase.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "external_endpoint", indexes = { @Index(name = "uuid_index", columnList = "uuid", unique = true) })
public class ExternalEndpoint {
	
	@Id
	@Column(name = "id_external_endpoint")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idExternalEndpoint;
	@Column(nullable=false)
	private String name;
	private String description;
	@Column(nullable=false)
	private String url;
	@Column(nullable=false)
	private String method;
	@Column(name = "uuid", unique = true, nullable = false)
	private String uuid;
	@ManyToOne
    @JoinColumn(name = "id_external_auth", nullable = true, updatable = true)
	private ExternalAuth externalAuth;
	@ManyToMany(mappedBy = "report")
	private Set<Role> roles;
	
	public Long getIdExternalEndpoint() {
		return idExternalEndpoint;
	}
	public void setIdExternalEndpoint(Long idExternalEndpoint) {
		this.idExternalEndpoint = idExternalEndpoint;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	

	public ExternalAuth getExternalAuth() {
		return externalAuth;
	}
	public void setExternalAuth(ExternalAuth externalAuth) {
		this.externalAuth = externalAuth;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	

	
	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}


	

}
