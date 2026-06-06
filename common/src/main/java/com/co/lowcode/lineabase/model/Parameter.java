package com.co.lowcode.lineabase.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/*@Entity
@Table(name = "parameter")*/
public class Parameter {
	
	private Long idParameter;
	private String name;
	private String code;
	private Long idParent;
	
	
	/**
	 * @return the idParameter
	 */
	@Id
	@GeneratedValue(/*generator = "parameter_id_seq", */strategy = GenerationType.SEQUENCE)
	//@SequenceGenerator(name = "parameter_id_seq", sequenceName = "parameter_id_seq", allocationSize=1)
	@Column(name = "id_parameter")
	public Long getIdParameter() {
		return idParameter;
	}

	public void setIdParameter(Long idParameter) {
		this.idParameter = idParameter;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Long getIdParent() {
		return idParent;
	}

	public void setIdParent(Long idParent) {
		this.idParent = idParent;
	}
	
}
