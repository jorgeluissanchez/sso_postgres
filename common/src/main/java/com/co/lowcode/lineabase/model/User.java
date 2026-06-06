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
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties
@Entity
@Table(name = "users")
public class User {
    private Long id;
    private String fullName;
    private String username;
    private String password;
    private String passwordConfirm;
    private Set<Role> roles;
    private Boolean active;
    private Boolean ldap;
    private String refreshToken;
    private String apiToken;

    private Version version;

	private String tokenActivation;
	private String tokenRestore;
	@Id
	//@SequenceGenerator(name = "user_id_seq", sequenceName = "user_id_seq")
	//@GeneratedValue(generator = "user_id_seq", strategy = GenerationType.IDENTITY)
	@GeneratedValue( strategy = GenerationType.IDENTITY)
	@Column(name = "id_user")
    public Long getId() {
        return id;
    }

	public User(){
		
	}
	
	
    public User(User user) {
    	this.id = user.getId();
    	this.password = user.getPassword();
    	this.username = user.getUsername();
    	this.active = user.isActive();
    	this.roles = user.getRoles();
    	this.ldap = user.isLdap();
	}

	public void setId(Long id) {
        this.id = id;
    }

	@Column(unique = true)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Transient
    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_users", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

	public Boolean isActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Boolean isLdap() {
		return ldap;
	}

	public void setLdap(Boolean ldap) {
		this.ldap = ldap;
	}

	

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}


	public String getTokenActivation() {
		return tokenActivation;
	}

	public void setTokenActivation(String tokenActivation) {
		this.tokenActivation = tokenActivation;
	}

	public String getTokenRestore() {
		return tokenRestore;
	}

	public void setTokenRestore(String tokenRestore) {
		this.tokenRestore = tokenRestore;
	}
	
	@OneToOne(mappedBy = "createdBy", cascade = CascadeType.ALL)
	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}
	
	
	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}


}