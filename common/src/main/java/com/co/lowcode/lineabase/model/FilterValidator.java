package com.co.lowcode.lineabase.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/*
@Entity
@Table(name = "filter_validator")*/
public class FilterValidator {

	public Long idValidator;
	private String name;
	private String message;
	private String type;
	private String detail;
	
	
	
	
	/**
	 * @return the idValidator
	 */
	@Id
	//@SequenceGenerator(name = "validator_id_seq", sequenceName = "validator_id_seq")
	@GeneratedValue(/*generator = "validator_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_validator")
	public Long getIdValidator() {
		return idValidator;
	}

	public void setIdValidator(Long idValidator) {
		this.idValidator = idValidator;
	}
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the detail
	 */
	public String getDetail() {
		return detail;
	}
	/**
	 * @param detail the detail to set
	 */
	public void setDetail(String detail) {
		this.detail = detail;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}	

}