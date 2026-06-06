package com.co.lowcode.audit.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
@Entity
@Table(name = "log")
public class Log {

	private Long id;
	private String entidad;
	private String usuario;
	private String evento;
	private String valor;
	private Date createdAt;
	
	
	@Id
	@SequenceGenerator(name = "log_id_seq", sequenceName = "log_id_seq")
	@GeneratedValue(generator = "log_id_seq", strategy = GenerationType.IDENTITY)
	@Column(name = "id_log")
    public Long getId() {
        return id;
    }


	public String getEntidad() {
		return entidad;
	}


	public void setEntidad(String entidad) {
		this.entidad = entidad;
	}


	public String getUsuario() {
		return usuario;
	}


	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}


	public String getEvento() {
		return evento;
	}


	public void setEvento(String evento) {
		this.evento = evento;
	}


	public String getValor() {
		return valor;
	}


	public void setValor(String valor) {
		this.valor = valor;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public Date getCreatedAt() {
		return createdAt;
	}


	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	
	
}
