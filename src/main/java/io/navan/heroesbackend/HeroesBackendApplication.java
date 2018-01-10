package io.navan.heroesbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;

@SpringBootApplication(exclude = RepositoryRestMvcAutoConfiguration.class)
public class HeroesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeroesBackendApplication.class, args);
	}
}
