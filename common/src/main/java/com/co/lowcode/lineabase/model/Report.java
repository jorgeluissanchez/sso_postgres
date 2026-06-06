package com.co.lowcode.lineabase.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@Entity
@Table(name = "report", indexes = { @Index(name = "uuid_index", columnList = "uuid", unique = true) })
public class Report {

	public Long idReport;
	private String name;
	@Column(columnDefinition = "TEXT")
	private String query;
	// Si es una tabla o gráfica
	private String type;
	private String description;
	private Set<Role> roles;
	
	private Integer public_end;
	
	private Integer captcha;
	
	
	@Column(name = "uuid", unique = true, nullable = false)
	private String uuid;

//	private Set<ReportDetail> reportDetail;
	
//	private Set<ReportFilter> reportFilter;
	
//	private Set<Chart> charts;
	
//	private Set<ReportAction> reportAction;
	
//	private Set<ReportStyle> reportStyle;


	/**
	 * @return the idReport
	 */
	@Id
	//@SequenceGenerator(name = "report_id_seq", sequenceName = "report_id_seq")
	@GeneratedValue(/*generator = "report_id_seq", */ strategy = GenerationType.IDENTITY)
	@Column(name = "id_report")
	public Long getIdReport() {
		return idReport;
	}

	public void setIdReport(Long idReport) {
		this.idReport = idReport;
	}
	
	
/*	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "report_report_action", joinColumns = @JoinColumn(name = "report_id"), inverseJoinColumns = @JoinColumn(name = "report_action_id"))
	public Set<ReportAction> getReportAction() {
		return reportAction;
	}

	public void setReportAction(Set<ReportAction> reportAction) {
		this.reportAction = reportAction;
	}
	

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "report_report_style", joinColumns = @JoinColumn(name = "report_id"), inverseJoinColumns = @JoinColumn(name = "report_style_id"))
	public Set<ReportStyle> getReportStyle() {
		return reportStyle;
	}

	public void setReportStyle(Set<ReportStyle> reportStyle) {
		this.reportStyle = reportStyle;
	}
*/

	@ManyToMany(mappedBy = "report")
	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	
	

	/*@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "report_report_detail", joinColumns = @JoinColumn(name = "report_id"), inverseJoinColumns = @JoinColumn(name = "report_detail_id"))
	public Set<ReportDetail> getReportDetail() {
		return reportDetail;
	}

	public void setReportDetail(Set<ReportDetail> reportDetail) {
		this.reportDetail = reportDetail;
	}

	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "report_report_filter", joinColumns = @JoinColumn(name = "report_id"), inverseJoinColumns = @JoinColumn(name = "report_filter_id"))
	public Set<ReportFilter> getReportFilter() {
		return reportFilter;
	}
	
	public void setReportFilter(Set<ReportFilter> reportFilter) {
		this.reportFilter = reportFilter;
	}
*/

	/**
	 * @return the charts
	 */
	/*@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "report_chart", joinColumns = @JoinColumn(name = "report_id"), inverseJoinColumns = @JoinColumn(name = "chart_id"))
	public Set<Chart> getCharts() {
		return charts;
	}

	
	public void setCharts(Set<Chart> charts) {
		this.charts = charts;
	}*/

	public Integer getPublic_end() {
		return public_end;
	}

	public void setPublic_end(Integer public_end) {
		this.public_end = public_end;
	}

	public Integer getCaptcha() {
		return captcha;
	}

	public void setCaptcha(Integer captcha) {
		this.captcha = captcha;
	}

}