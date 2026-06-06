package com.co.lowcode.lineabase.model;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
/*
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@Entity
@Table(name = "chart")*/
public class Chart {
	
	public Long idChart;
	private String name;
	private Set<Axes> axes;
	private Set<Series> series;
	private String query;
	
	/**
	 * @return the idChart
	 */
	@Id
	//@SequenceGenerator(name = "chart_id_seq", sequenceName = "chart_id_seq")
	@GeneratedValue(/*generator = "chart_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_chart")
	public Long getIdChart() {
		return idChart;
	}

	public void setIdChart(Long idChart) {
		this.idChart = idChart;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "chart_axes", joinColumns = @JoinColumn(name = "chart_id"), inverseJoinColumns = @JoinColumn(name = "axes_id"))
	public Set<Axes> getAxes() {
		return axes;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "chart_series", joinColumns = @JoinColumn(name = "chart_id"), inverseJoinColumns = @JoinColumn(name = "series_id"))
	public Set<Series> getSeries() {
		return series;
	}

	public void setAxes(Set<Axes> axes) {
		this.axes = axes;
	}

	public void setSeries(Set<Series> series) {
		this.series = series;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * @param query the query to set
	 */
	public void setQuery(String query) {
		this.query = query;
	}
	
}