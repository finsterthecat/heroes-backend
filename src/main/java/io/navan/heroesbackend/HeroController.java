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

	/**
	 * Create a hero. Returned hero will have the auto-generated id of the new hero.
	 * 
	 * @param hero
	 * @return the created hero
	 */
	@PostMapping(consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public Hero createHero(@RequestBody Hero hero) {
		LOG.debug("createHero: {}", hero.getName());
		Hero createdHero = heroRepository.save(hero);
		LOG.debug("Created hero {} with id {}",
				createdHero.getName(), createdHero.getId());
		return createdHero;
	}

	/**
	 * Retrieve all heroes
	 * 
	 * @return iterable with all heroes
	 */
	@GetMapping(produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Iterable<Hero> allHeroes() {
		LOG.debug("allHeroes");
		return heroRepository.findAll();
	}

	/**
	 * Get a hero by id.
	 * 
	 * @param id the hero's id
	 * @return the hero
	 */
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

	/**
	 * Update a hero. Hero must exist for id.
	 * 
	 * @param id The id of the hero to update
	 * @param hero The hero value
	 * @throws ResourceNotFoundException if not found.
	 */
	@PutMapping(value = "/{id}", consumes = "application/json",
			produces = "application/json")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateHero(@PathVariable Long id, @RequestBody Hero hero) {
		//Retrieve hero first. This is the only way to ensure hero already exists prior to saving.
		Hero currentHero = heroRepository.findOne(id);
		if (currentHero == null) {
			throw new ResourceNotFoundException("Hero is not found for id=" + id);
		}
		LOG.debug("updateHero: modified name from {} to {}",
				currentHero.getName(), hero.getName());
		currentHero.setName(hero.getName());
		this.heroRepository.save(currentHero);
	}
	
	/**
	 * Delete hero
	 * @param id
	 * @throws ResourceNotFoundException if not found.
	 */
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
    
    /**
     * Find hero with name containing string (not case sensitive)
     * @param name The string to search for.
     * @return Iterable with heroes with matching names.
     */
    @GetMapping(value = "/search/name", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public Iterable<Hero> findByName(@RequestParam("contains") String name) {
    		LOG.debug("findByName >{}<", name);
    		return heroRepository.findByName(name);
    }
}
