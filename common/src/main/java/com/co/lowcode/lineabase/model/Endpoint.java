package com.co.lowcode.lineabase.model;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author danielromero
 *
 */
@Entity
@Table(name = "endpoint")
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class Endpoint {

	private Long id;
	private String path;
	private String method;
	private Integer numberParams;
	private String description;
	
	
	@Id
	//@SequenceGenerator(name = "endpoint_id_seq", sequenceName = "endpoint_id_seq")
	@GeneratedValue(/*generator = "endpoint_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_endpoint")
    public Long getId() {
        return id;
    }
	
	public Set<MicroService> microservice;

	
	public void setId(Long id) {
		this.id = id;
	}

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	 @JoinTable(name = "endpoint_microservice", joinColumns = @JoinColumn(name = "endpoint_id"), 
	 inverseJoinColumns = @JoinColumn(name = "microservice_id"))
	public Set<MicroService> getMicroservice() {
		return microservice;
	}

	public void setMicroservice(Set<MicroService> microservice) {
		this.microservice = microservice;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Integer getNumberParams() {
		return numberParams;
	}

	public void setNumberParams(Integer numberParams) {
		this.numberParams = numberParams;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


}