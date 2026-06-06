package com.co.lowcode.lineabase.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "app")
public class App {
	
	private Long id;
	@Column(unique = true)
	private String name;
	private String description;
	private String uri;
	//uuid que indica el screen que inicia la app
	private String rootScreen;
	private Set<MicroService> microservices;
	private Set<Route> routes;
	//private Set<Screen> screens;
	private List<Version> vesions;

	private Date createdAt;
	private Date updateAt;
	private String owner;
	
	@Id
	//@SequenceGenerator(name = "app_id_seq", sequenceName = "app_id_seq")
	@GeneratedValue(/*generator = "app_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_app")
    public Long getId() {
        return id;
    }
	
	
	@ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "app_microservice", joinColumns = @JoinColumn(name = "app_id"), inverseJoinColumns = @JoinColumn(name = "id_microservice"))
	public Set<MicroService> getMicroservicios() {
		return microservices;
	}

	public void setMicroservicios(Set<MicroService> microservicios) {
		this.microservices = microservicios;
	}
	
	@ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "app_route", joinColumns = @JoinColumn(name = "app_id"), inverseJoinColumns = @JoinColumn(name = "route_id"))
	public Set<Route> getRoutes() {
		return routes;
	}
	
	
	
	public void setRoutes(Set<Route> routes) {
		this.routes = routes;
	}
	




	public void setId(Long id) {
		this.id = id;
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

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "app")
	public List<Version> getVesions() {
		return vesions;
	}


	public void setVesions(List<Version> vesions) {
		this.vesions = vesions;
	}

/*	@ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "app_screen", joinColumns = @JoinColumn(name = "app_id"), inverseJoinColumns = @JoinColumn(name = "screen_id"))
	public Set<Screen> getScreens() {
		return screens;
	}


	public void setScreens(Set<Screen> screens) {
		this.screens = screens;
	}
*/

	public String getRootScreen() {
		return rootScreen;
	}


	public void setRootScreen(String rootScreen) {
		this.rootScreen = rootScreen;
	}


	public Date getUpdateAt() {
		return updateAt;
	}


	public void setUpdateAt(Date updateAt) {
		this.updateAt = updateAt;
	}


	public Date getCreatedAt() {
		return createdAt;
	}


	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}


	public String getOwner() {
		return owner;
	}


	public void setOwner(String owner) {
		this.owner = owner;
	}


	public String getUri() {
		return uri;
	}


	public void setUri(String uri) {
		this.uri = uri;
	}
	
	

}