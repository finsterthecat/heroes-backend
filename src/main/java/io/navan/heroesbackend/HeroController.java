package io.navan.heroesbackend;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping(value = "heroes")
@Api(tags= {"heroes"})
public class HeroController {

    @Autowired
    HeroRepository heroRepository;

    private static final Logger LOG = LoggerFactory.getLogger(HeroController.class);

    /**
     * Create a hero. Returned hero will have the auto-generated id of the new hero.
     * 
     * @param hero The hero to create
     * @return the created hero
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Create a hero resource.",
                    notes = "Create hero returning the new Hero")
    public Hero createHero(@RequestBody Hero hero,
            HttpServletRequest request, HttpServletResponse response) {
        LOG.debug("createHero: {}", hero.getName());
        Hero createdHero = heroRepository.save(hero);
        response.setHeader("Location", request
                .getRequestURL()
                .append("/")
                .append(createdHero.getId()).toString());
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
    @ApiOperation(value = "Get all heroes.", notes = "Get a list of all heroes.")
    public @ResponseBody Iterable<Hero> allHeroes(
            @ApiParam(value = "Search by this stuff", required = false)
            HeroesRequestFilter heroesRequestFilter) {
        LOG.debug("allHeroes filter:{}", heroesRequestFilter);
        if (heroesRequestFilter.getName() != null) {
            return heroRepository.findByName(heroesRequestFilter.getName());
        }
        else {
            return heroRepository.findAll();
        }
    }

    /**
     * Get a hero by id.
     * 
     * @param id
     *            the hero's id
     * @return the hero
     */
    @GetMapping(value = "/{id:\\d+}", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Get a single hero.", notes = "Get hero By ID.")
    @ApiResponses(value = {
            @ApiResponse(code=404, message="Hero not found.")
    })
    public @ResponseBody Hero singleHero(
            @ApiParam(value = "The ID of the hero.", required = true)
            @PathVariable Long id) {
        LOG.debug("singleHero for id {}", id);
        Hero hero = heroRepository.findOne(id);
        if (hero == null) {
            throw new ResourceNotFoundException("Hero not found");
        }
        return hero;
    }

    /**
     * Update a hero. Hero must exist for id.
     * 
     * @param id
     *            The id of the hero to update
     * @param hero
     *            The hero value
     * @throws ResourceNotFoundException
     *             if not found.
     */
    @PutMapping(value = "/{id:\\d+}",
            consumes = "application/json",
            produces = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Update an existing hero resource.",
            notes = "Update Hero for id.")
    @ApiResponses(value = {
        @ApiResponse(code=400, message="Validation Errors"),
        @ApiResponse(code=404, message="Hero not found")
    })
    public void updateHero(
            @ApiParam(value = "The ID of the hero resource", required = true)
            @PathVariable Long id,
            @RequestBody Hero hero) {
        // Retrieve hero first. This is the only way to ensure hero already exists prior
        // to saving.
        Hero currentHero = heroRepository.findOne(id);
        if (currentHero == null) {
            throw new ResourceNotFoundException("Hero not found");
        }
        LOG.debug("updateHero: modified name from {} to {}",
                currentHero.getName(), hero.getName());
        currentHero.setName(hero.getName());
        this.heroRepository.save(currentHero);
    }

    /**
     * Delete hero
     * 
     * @param id The id
     * @throws ResourceNotFoundException
     *             if not found.
     */
    @ApiOperation(value = "Delete a hero resource.",
            notes = "Delete hero with id.")
    @ApiResponses(value = {
        @ApiResponse(code=404, message="Not Found")
    })
    @DeleteMapping(value = "/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHero(
            @ApiParam(value = "The ID of the hero resource", required = true)
            @PathVariable Long id) {
        LOG.debug("delete >{}<", id);
        try {
            heroRepository.delete(id);
        } catch (EmptyResultDataAccessException e1) {
            throw new ResourceNotFoundException("Hero not found");
        }
    }

    /**
     * Find hero with name containing string (not case sensitive).
     * 
     * @deprecated Use allHeroes
     * @param name The string to search for.
     * @return Iterable with heroes with matching names.
     */
    @GetMapping(value = "/search/name", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Find hero resources by name.",
        notes= "Find hero resources with names that contain specified text.")
    public Iterable<Hero> findByName(
            @ApiParam(value = "Search for heroes with name containing", required=true)
            @RequestParam("contains") String name) {
        LOG.debug("findByName >{}<", name);
        return heroRepository.findByName(name);
    }
}
