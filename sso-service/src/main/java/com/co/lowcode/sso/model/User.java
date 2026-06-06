package com.co.lowcode.sso.model;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

public class User {

	@NotNull
	@NotBlank
	private String FULLNAME;
	@NotNull
	@NotBlank
    private String USERNAME;
    private String PASSWORD;
    private String PASSWORDCONFIRM;
    private Boolean ACTIVE;
    
    private Boolean LDAP;
    private String refreshToken;
    
    private String tokenActivation;
    
    private Integer CODENTIDAD;

	public String getFULLNAME() {
		return FULLNAME;
	}

	public void setFULLNAME(String fULLNAME) {
		FULLNAME = fULLNAME;
	}

	public String getUSERNAME() {
		return USERNAME;
	}

	public void setUSERNAME(String uSERNAME) {
		USERNAME = uSERNAME;
	}

	public String getPASSWORD() {
		return PASSWORD;
	}

	public void setPASSWORD(String pASSWORD) {
		PASSWORD = pASSWORD;
	}

	public String getPASSWORDCONFIRM() {
		return PASSWORDCONFIRM;
	}

	public void setPASSWORDCONFIRM(String pASSWORDCONFIRM) {
		PASSWORDCONFIRM = pASSWORDCONFIRM;
	}

	public Boolean getACTIVE() {
		return ACTIVE;
	}

	public void setACTIVE(Boolean aCTIVE) {
		ACTIVE = aCTIVE;
	}

	public Boolean getLDAP() {
		return LDAP;
	}

	public void setLDAP(Boolean lDAP) {
		LDAP = lDAP;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getTokenActivation() {
		return tokenActivation;
	}

	public void setTokenActivation(String tokenActivation) {
		this.tokenActivation = tokenActivation;
	}

	public Integer getCODENTIDAD() {
		return CODENTIDAD;
	}

	public void setCODENTIDAD(Integer cODENTIDAD) {
		CODENTIDAD = cODENTIDAD;
	}
    
    
}
