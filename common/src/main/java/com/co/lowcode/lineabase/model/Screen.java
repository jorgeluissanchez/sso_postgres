package com.co.lowcode.lineabase.model;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

//@Entity
//@Table(name = "screen")
public class Screen {
	
	private Long id;
	@Column(unique = true, nullable = false)
	private String uuid;
	private String name;
	private Set<Component> components;
	private Date createdAt;
	private Date updatedAt;
	
	private String initComponent;

	

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY)
	@Column(name = "id_screen")
    public Long getId() {
        return id;
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

	@ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "screen_component", 
    joinColumns = @JoinColumn(name = "id_screen"), 
    inverseJoinColumns = @JoinColumn(name = "id_component"))
	public Set<Component> getComponents() {
		return components;
	}

	public void setComponents(Set<Component> components) {
		this.components = components;
	}


	public String getInitComponent() {
		return initComponent;
	}

	public void setInitComponent(String initComponent) {
		this.initComponent = initComponent;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
