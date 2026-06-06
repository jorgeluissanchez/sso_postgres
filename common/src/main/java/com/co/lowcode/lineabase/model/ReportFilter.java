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

/*@Entity
@Table(name = "report_filter")*/
public class ReportFilter {
	
	private Long idReportFilter;
	private Integer columnOrder;
	private String name;
	private String label;
	private String type;
	
	private String uuidParent;
	private String nameIdParent;
	private String query;
	private String endpoint;
	//IdParameter de la tabla Parameter
	private Long idParameter;
	// Tipo de metodo de consulta query,endpoint,parameter
	@Column(nullable=false)
	private String method;
	
	private Set<FilterValidator> filterValidator;
	
	@Column(name = "uuid", unique = true, nullable = false)
	private String uuid;
	
	
	/**
	 * @return the idReportFilter
	 */
	@Id
	//@SequenceGenerator(name = "report_filter_id_seq", sequenceName = "report_filter_id_seq")
	@GeneratedValue(/*generator = "report_filter_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_report_filter")
	public Long getIdReportFilter() {
		return idReportFilter;
	}

	public void setIdReportFilter(Long idReportFilter) {
		this.idReportFilter = idReportFilter;
	}
	
	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "report_filter_filter_validator", joinColumns = @JoinColumn(name = "report_filter_id"), inverseJoinColumns = @JoinColumn(name = "filter_validator_id"))
	public Set<FilterValidator> getFilterValidator() {
		return filterValidator;
	}

	public void setFilterValidator(Set<FilterValidator> filterValidator) {
		this.filterValidator = filterValidator;
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
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getNameIdParent() {
		return nameIdParent;
	}
	public void setNameIdParent(String nameIdParent) {
		this.nameIdParent = nameIdParent;
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

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUuidParent() {
		return uuidParent;
	}

	public void setUuidParent(String uuidParent) {
		this.uuidParent = uuidParent;
	}

	public Long getIdParameter() {
		return idParameter;
	}

	public void setIdParameter(Long idParameter) {
		this.idParameter = idParameter;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	
	
	
}