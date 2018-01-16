package io.navan.heroesbackend;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
 
@Entity
public class Hero {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
 
    @NotNull(message = "Name is required")
    private String name;
	
	public Hero() {}
	
	public Hero(String name) {
		this.name = name;
	}
  
    public Long getId() {
        return id;
    }
 
    public void setId(Long id) {
        this.id = id;
    }
 
    public void setName(String name) {
        this.name = name;
    }
     
    public String getName() {
        return name;
    }
}
