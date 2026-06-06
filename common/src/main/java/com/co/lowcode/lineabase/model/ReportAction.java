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
@Table(name = "report_action")*/
public class ReportAction {
	
	private Long id;
	
	private String button;
	private String uuidReport;
	private String type;
	private Boolean confirm;
	

	@Id
	//@SequenceGenerator(name = "report_action_id_seq", sequenceName = "report_action_id_seq")
	@GeneratedValue(/*generator = "report_action_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_report_action")
	public Long getId() {
		return id;
	}


	public String getButton() {
		return button;
	}


	public void setButton(String button) {
		this.button = button;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public String getUuidReport() {
		return uuidReport;
	}


	public void setUuidReport(String uuidReport) {
		this.uuidReport = uuidReport;
	}


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public Boolean getConfirm() {
		return confirm;
	}


	public void setConfirm(Boolean confirm) {
		this.confirm = confirm;
	}
}