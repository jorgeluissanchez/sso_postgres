package com.co.lowcode.lineabase.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL) 
@JsonIgnoreProperties
@Entity
@Table(name = "route")
public class Route {
	

	public Route() {
	}

	private Long idRoute;
	
	private String name;
	
	private String path;
	//initialRoute, loginPage, initialAuthPage, authPage
	private String type;
	
	private Long idParent;
	
	private String icon;
	
	private Integer menuOrder;
	
	
	//private Component component;
	
    private Set<Role> roles;
    
	@ManyToMany(mappedBy = "routes")
    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Id
	//@SequenceGenerator(name = "route_id_seq", sequenceName = "route_id_seq")
	@GeneratedValue( strategy = GenerationType.IDENTITY)
	@Column(name = "id_route")
	public Long getIdRoute() {
		return idRoute;
	}

	public void setIdRoute(Long idOpcion) {
		this.idRoute = idOpcion;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/*@OneToOne
    @MapsId(value = "component_id")
	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}
*/
	public Long getIdParent() {
		return idParent;
	}

	public void setIdParent(Long idParent) {
		this.idParent = idParent;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Integer getMenuOrder() {
		return menuOrder;
	}

	public void setMenuOrder(Integer menuOrder) {
		this.menuOrder = menuOrder;
	}
	
	

}
