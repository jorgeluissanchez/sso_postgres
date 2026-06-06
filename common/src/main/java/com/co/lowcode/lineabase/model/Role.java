  package com.co.lowcode.lineabase.model;

import java.math.BigInteger;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;

import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "role")
public class Role implements GrantedAuthority {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id_role")
	private Long id;

	@NotEmpty
	private String name;
	
	 @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	 @JoinTable(name = "role_route", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "route_id"))
	public Set<Route> routes;
	 
	 @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	 @JoinTable(name = "role_report", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "report_id"))
	public Set<Report> report;
	 
	 @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	 @JoinTable(name = "role_endpoint", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "endpoint_id"))
	public Set<Endpoint> endpoint;
	 
	 @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	 @JoinTable(name = "role_app", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "app_id"))
	public Set<App> app;
	 
	/* @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	 @JoinTable(name = "role_screen", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "screen_id"))
	public Set<Screen> screen;
*/
	 @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	 @JoinTable(name = "role_external_endpoint", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "external_endpoint_id"))
	public Set<ExternalEndpoint> externalEndpoint;
	 
	 @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	 @JoinTable(name = "role_file", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "file_id"))
	public Set<File> file;


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@Override
	public String getAuthority() {
		return name;
	}	
	
	public void setAuthority(String name) {
		this.name = name;
	}
	
	public Set<Route> getRoutes() {
		return routes;
	}

	public void setRoutes(Set<Route> routes) {
		this.routes = routes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Endpoint> getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Set<Endpoint> endpoint) {
		this.endpoint = endpoint;
	}

	public Set<Report> getReport() {
		return report;
	}

	public void setReport(Set<Report> report) {
		this.report = report;
	}

	/*public Set<Screen> getScreen() {
		return screen;
	}

	public void setScreen(Set<Screen> screen) {
		this.screen = screen;
	}*/

	public Set<App> getApp() {
		return app;
	}

	public void setApp(Set<App> app) {
		this.app = app;
	}

	public Set<File> getFile() {
		return file;
	}

	public void setFile(Set<File> file) {
		this.file = file;
	}
	
	

}