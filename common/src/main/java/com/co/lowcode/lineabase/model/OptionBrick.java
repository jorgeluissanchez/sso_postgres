package com.co.lowcode.lineabase.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
/*
@Entity
@Table(name = "option_brick")*/
public class OptionBrick {
	@Id
	@Column(name = "id_option_brick")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idOptionBrick;
	
	private String label;
	private String icon;
	@Column(unique = true)
	private String uuid;
	
	@ManyToOne
    @JoinColumn(name = "id_building_brick", nullable = false, updatable = false)
	private BuildingBrick buildings;
	
	
	public Long getIdOptionBrick() {
		return idOptionBrick;
	}
	
	
	public BuildingBrick getBuildings() {
		return buildings;
	}
	public void setBuildings(BuildingBrick buildings) {
		this.buildings = buildings;
	}
	public void setIdOptionBrick(Long idOptionBrick) {
		this.idOptionBrick = idOptionBrick;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}


}
