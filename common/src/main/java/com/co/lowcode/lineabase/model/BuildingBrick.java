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
/*
@Entity
@Table(name = "building_brick")*/
public class BuildingBrick {
	@Id
	@Column(name = "id_building_brick")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idBuildingBrick;
	
	private String label;
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "buildings")
	private List<OptionBrick> options;

	public Long getIdBuildingBrick() {
		return idBuildingBrick;
	}

	public void setIdBuildingBrick(Long idBuildingBrick) {
		this.idBuildingBrick = idBuildingBrick;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	
	
	public List<OptionBrick> getOptions() {
		return options;
	}

	public void setOptionBrick(List<OptionBrick> options) {
		this.options = options;
	}
	
}
