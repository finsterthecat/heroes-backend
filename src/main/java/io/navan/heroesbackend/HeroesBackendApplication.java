package io.navan.heroesbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = RepositoryRestMvcAutoConfiguration.class)
@ComponentScan(basePackages = "io.navan")
public class HeroesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeroesBackendApplication.class, args);
	}
}
