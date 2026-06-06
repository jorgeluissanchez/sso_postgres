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
@Table(name = "report_detail")*/
public class ReportDetail {
	public Long id;

	
	private String name;
	private String alias;
	private String mask;
	private Integer columnOrder;
	
	@Id
//	@SequenceGenerator(name = "report_detail_id_seq", sequenceName = "report_detail_id_seq")
	@GeneratedValue(/*generator = "report_detail_id_seq", */strategy = GenerationType.IDENTITY)
	@Column(name = "id_report_detail")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	

	public Integer getColumnOrder() {
		return columnOrder;
	}

	public void setColumnOrder(Integer columnOrder) {
		this.columnOrder = columnOrder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getMask() {
		return mask;
	}

	public void setMask(String mask) {
		this.mask = mask;
	}

	
	
	
	
}
