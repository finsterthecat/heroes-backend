package io.navan.heroesbackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "heroes")
public class HeroController {

	@Autowired
	HeroRepository heroRepository;

	private static final Logger LOG = LoggerFactory.getLogger(HeroController.class);

	@PostMapping(consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public Hero createHero(Hero hero) {
		LOG.debug("createHero: {}", hero.getName());
		Hero createdHero = heroRepository.save(hero);
		LOG.debug("Created hero {} with id {}",
				createdHero.getName(), createdHero.getId());
		return createdHero;
	}

	@GetMapping(produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Iterable<Hero> allHeroes() {
		LOG.debug("allHeroes");
		return heroRepository.findAll();
	}

	@GetMapping(value = "/{id}", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Hero singleHero(@PathVariable Long id) {
		LOG.debug("singleHero for id {}", id);
		Hero hero = heroRepository.findOne(id);
		if (hero == null) {
			throw new ResourceNotFoundException("Hero not found for id: " + id);
		}
		return hero;
	}

	@PutMapping(value = "/{id}", consumes = "application/json",
			produces = "application/json")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateHero(@PathVariable Long id, @RequestBody Hero hero) {
		Hero currentHero = heroRepository.findOne(id);
		if (currentHero == null) {
			throw new ResourceNotFoundException("Hero is not found for id=" + id);
		}
		LOG.debug("updateHero: modified name from {} to {}",
				currentHero.getName(), hero.getName());
		currentHero.setName(hero.getName());
		//this.heroRepository.save(currentHero);
	}
	
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteHero(@PathVariable Long id) {
    		LOG.debug("delete >{}<", id);
		try {
			heroRepository.delete(id);
		} catch (EmptyResultDataAccessException e1) {
			throw new ResourceNotFoundException("Cannot delete Hero with id "
					+ id + ". Not found", e1);
		}
	}
    
    @GetMapping(value = "/search/name", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public Iterable<Hero> findByName(@RequestParam("contains") String name) {
    		LOG.debug("findByName >{}<", name);
    		return heroRepository.findByName(name);
    }
}
