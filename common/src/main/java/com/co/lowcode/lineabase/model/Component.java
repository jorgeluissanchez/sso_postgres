package com.co.lowcode.lineabase.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/*@Entity
@Table(name = "component")*/
public class Component {
	
	private Long id;
	private String uuid;
	

	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_component")
    public Long getId() {
        return id;
    }

	public void setId(Long id) {
		this.id = id;
	}

	@Column(unique = true)
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	
  //  private Route route;
    
    
    //@OneToOne(mappedBy = "component", cascade = CascadeType.ALL)
	/*public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}
*/

	

}