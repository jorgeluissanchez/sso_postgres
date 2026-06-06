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
@Table(name = "axes")*/
public class Axes {
	
	public Long idAxes;
	private String query;
	// tipo de categoria  de la abscisa acepta Category o Value, si es Category toma los valores fijos definidos en el query o endpoint
	private String type;
	// define el formato de los valores de las abscisas ejemplo %,$,ºC, \n
	private String formatter;
	// define la abscisa x o y
	private String axis;
	// define si la etiqueta abscisa se quiere dentro o afuera de la grafica es decir si es un eje Y e inside las etiquetas se pintaran al lado derecho.
	private Boolean inside;
	// nombre de la columna asignada al eje
	private String nameColumn;
	
	private String name;
	
	private String position;
	
	private String offset;
	
	@Id
	//@SequenceGenerator(name = "axes_id_seq", sequenceName = "axes_id_seq")
	@GeneratedValue(/*generator = "axes_id_seq", */ strategy = GenerationType.IDENTITY)
	@Column(name = "id_axes")
	public Long getIdAxes() {
		return idAxes;
	}

	public void setIdAxes(Long idAxes) {
		this.idAxes = idAxes;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFormatter() {
		return formatter;
	}

	public void setFormatter(String formatter) {
		this.formatter = formatter;
	}

	public String getAxis() {
		return axis;
	}

	public void setAxis(String axis) {
		this.axis = axis;
	}

	public Boolean getInside() {
		return inside;
	}

	public void setInside(Boolean inside) {
		this.inside = inside;
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
	
	
}
