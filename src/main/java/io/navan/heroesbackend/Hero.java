package io.navan.heroesbackend;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModelProperty;
 
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames= "name")})
public class Hero {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ApiModelProperty(notes = "The database generated Hero ID")
    private Long id;
 
    @NotNull(message = "Name is required")
    @Size(min = 1, max=20, message = "Name must be between 1 and 20 characters long")
    @ApiModelProperty(notes = "Hero's Name")
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
