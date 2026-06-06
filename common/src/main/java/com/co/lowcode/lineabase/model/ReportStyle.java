package com.co.lowcode.lineabase.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/*@Entity
@Table(name = "report_style")*/
public class ReportStyle {
	@Id
	public Long id;
	private String words;
	private String css;
	private String columnref;
	
	
	@Id
	//@SequenceGenerator(name = "style_id_seq", sequenceName = "style_id_seq")
	@GeneratedValue(/*generator = "style_id_seq",*/ strategy = GenerationType.IDENTITY)
	@Column(name = "id_style")
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	
	public String getWords() {
		return words;
	}
	
	public void setWords(String words) {
		this.words = words;
	}
	
	public String getCss() {
		return css;
	}
	
	public void setCss(String css) {
		this.css = css;
	}


	public String getColumnref() {
		return columnref;
	}

	public void setColumnref(String columnref) {
		this.columnref = columnref;
	}

}
