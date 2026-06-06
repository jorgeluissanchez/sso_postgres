package com.co.lowcode.lineabase.model;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
/*
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@Entity
@Table(name = "series")*/
//Entidad donde se almacenan los valores a graficar
public class Series {
	
	
	public Long idSeries;
	private String name;
	private String type;
	private String query;
	private String endpoint;
	private String radius1;
	private String radius2;
	private Boolean smooth;
	private String nameColumn;
	private String valueColumn;
	
	@Id
	//@SequenceGenerator(name = "series_id_seq", sequenceName = "series_id_seq")
	@GeneratedValue(/*generator = "series_id_seq", */strategy = GenerationType.IDENTITY)
	@Column(name = "id_series")
	public Long getIdSeries() {
		return idSeries;
	}
	
	public void setIdSeries(Long idSeries) {
		this.idSeries = idSeries;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getRadius1() {
		return radius1;
	}
	public void setRadius1(String radius1) {
		this.radius1 = radius1;
	}
	public String getRadius2() {
		return radius2;
	}
	public void setRadius2(String radius2) {
		this.radius2 = radius2;
	}
	public Boolean getSmooth() {
		return smooth;
	}
	public void setSmooth(Boolean smooth) {
		this.smooth = smooth;
	}

	/**
	 * @return the nameColumn
	 */
	public String getNameColumn() {
		return nameColumn;
	}

	/**
	 * @param nameColumn the nameColumn to set
	 */
	public void setNameColumn(String nameColumn) {
		this.nameColumn = nameColumn;
	}

	/**
	 * @return the valueColumn
	 */
	public String getValueColumn() {
		return valueColumn;
	}

	/**
	 * @param valueColumn the valueColumn to set
	 */
	public void setValueColumn(String valueColumn) {
		this.valueColumn = valueColumn;
	}
	
	
}
