package com.co.lowcode.lineabase.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "version")
public class Version {
	
	private Long id;
	@Column(unique = true)
	private String uuid;
	private Date createdAt;
	private Date updatedAt;
	private User createdBy;
	private User updatedBy;
	private String description;
	
    private App app;
	
	@Column(unique = true)
	private String name;
	
	
	@Id
	//@SequenceGenerator(name = "version_id_seq", sequenceName = "version_id_seq")
	@GeneratedValue(/*generator = "version_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_version")
    public Long getId() {
        return id;
    }

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	@Column(name = "created_at")
	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public void setId(Long id) {
		this.id = id;
	}
	@Column(name = "updated_at")
	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	@OneToOne
    @MapsId(value = "created_by")
	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	@OneToOne
    @MapsId(value = "updated_by")
	public User getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
    @JoinColumn(name = "fk_app", nullable = false, updatable = false)
	public App getApp() {
		return app;
	}

	public void setApp(App app) {
		this.app = app;
	}
	
	

}
