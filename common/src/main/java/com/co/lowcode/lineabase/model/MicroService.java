package com.co.lowcode.lineabase.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "microservice")
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class MicroService {

	private Long id;
	private String description;
	@Column(unique=true)
	private String serviceId;
	
	
	/**
	 * parametro para asignarle el host donde esta ubicado el servicio es opcional, si existe el service id no aplica sample localhost
	 */
	private String targetURLHost;
	/**
	 * parametro para asignarle el puerto donde esta ubicado el servicio es opcional, si existe el service id no aplica 
	 */
	private String targetURLPort;
	/**
	 * parametro para asignarle lo que le sigue a la ruta para consumir el servicio
	 */
	private String targetURIPath;
	
	/**
	 * parametro para asignarle la ruta para consumir el servicio sample /api1
	 */
	private String requestURI;
	
	@Column(name="created_date", columnDefinition="true")
	private Date createdDate;
	
	
	@Id
	//@SequenceGenerator(name = "microservice_id_seq", sequenceName = "microservice_id_seq")
	@GeneratedValue(/*generator = "microservice_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_microservice")
    public Long getId() {
        return id;
    }

	public void setId(Long id) {
		this.id = id;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getTargetURLHost() {
		return targetURLHost;
	}

	public void setTargetURLHost(String targetURLHost) {
		this.targetURLHost = targetURLHost;
	}

	public String getTargetURLPort() {
		return targetURLPort;
	}

	public void setTargetURLPort(String targetURLPort) {
		this.targetURLPort = targetURLPort;
	}

	public String getTargetURIPath() {
		return targetURIPath;
	}

	public void setTargetURIPath(String targetURIPath) {
		this.targetURIPath = targetURIPath;
	}

	public String getRequestURI() {
		return requestURI;
	}

	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	
}
