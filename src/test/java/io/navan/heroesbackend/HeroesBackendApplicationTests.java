package io.navan.heroesbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.navan.system.ValidationError;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class HeroesBackendApplicationTests {

    private static final String BASE_URL = "/heroes/";

    private static final Logger LOG = LoggerFactory.getLogger(HeroesBackendApplicationTests.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mvc;

    // Used for converting heroes to/from JSON
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Start with one row in the Hero table prior to each test.
     */
    @Before
    public void initTests() {
        // Always start from known state, in this case 1 row in hero table.
        jdbcTemplate.execute("delete from hero; insert into Hero(name) values ('Superman');");
    }

    @Test
    public void contextLoads() {
        assertThat(jdbcTemplate).isNotNull();
        assertThat(mvc).isNotNull();
    }

    /**
     * Should be One Superhero named Superman
     * 
     * @throws Exception
     */
    @Test
    public void shouldStartWithOneSuperheroSuperman() throws Exception {
        MvcResult result = invokeAllHeroes()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Superman")))
                .andReturn();

        Hero[] heroes = fromJsonResult(result, Hero[].class);
        LOG.debug("Superman's id: {}", heroes[0].getId());
    }

    /**
     * Should get a Superhero by id.
     * 
     * @throws Exception
     */
    @Test
    public void shouldGetSuperhero() throws Exception {
        Hero[] heroes = getAllHeroes();
        assertThat(heroes.length).isEqualTo(1);

        invokeGetHero(heroes[0].getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Superman")));
    }

    /**
     * Should get an internal server error when get Superhero by non-numeric id.
     * 
     * @throws Exception
     */
    @Test
    public void shouldNotFoundGetSuperheroWithBadId() throws Exception {
        mvc.perform(get(BASE_URL + "xxx").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Should get an internal server error when update Superhero by non-numeric id.
     * 
     * @throws Exception
     */
    @Test
    public void shouldNotFoundUpdateSuperheroWithBadId() throws Exception {
        mvc.perform(put(BASE_URL + "xxx")
                .content(toJson(new Hero("Stupor Man")))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Should get a HTTP Status 404 when Get Superhero by id is not found.
     * 
     * @throws Exception
     */
    @Test
    public void should404ForNotfoundSuperhero() throws Exception {
        Hero[] heroes = getAllHeroes();
        assertThat(heroes.length).isEqualTo(1);

        // Only one superhero, so its id plus 1 must not exist...

        invokeGetHero(heroes[0].getId() + 1)
                .andExpect(status().isNotFound());
    }

    /**
     * Create Superhero will return the created superhero.
     * 
     * @throws Exception
     */
    @Test
    public void shouldCreateSuperhero() throws Exception {
        String heroName = "Company Man";
        byte[] heroJson = toJson(new Hero(heroName));
        MvcResult results = invokeCreateHero(heroJson)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(heroName)))
                .andReturn();
        Hero hero = fromJsonResult(results, Hero.class);

        invokeGetHero(hero.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(heroName)));
    }

    /**
     * Create Superhero will return the created superhero.
     * 
     * @throws Exception
     */
    @Test
    public void shouldBadRequestCreateSuperheroTwice() throws Exception {
        String heroName = "Company Man";
        byte[] heroJson = toJson(new Hero(heroName));
        invokeCreateHero(heroJson)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(heroName)));
        invokeCreateHero(heroJson)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].entity", is(Hero.class.getName())))
                .andExpect(jsonPath("$.errors[0].property", is("name")))
                .andExpect(jsonPath("$.errors[0].message", is("Already Exists")));
    }

    @Test
    public void shouldBadRequestCreateSuperheroWithBadName() throws Exception {
        LOG.debug("CreateMissingName");
        MvcResult results;
        byte[] nullHeroJson = toJson(new Hero(null));
        results = invokeCreateHero(nullHeroJson).andExpect(status().isBadRequest()).andReturn();
        checkInvalidNameErrorResponse(results, "name is required");
        results = invokeCreateHero(toJson(new Hero("")))
                .andExpect(status().isBadRequest())
                .andReturn();
        checkInvalidNameErrorResponse(results, "name must be between 1 and 20 characters long");
    }

    /**
     * Create and then update a Hero and retrieve to make sure the update was
     * applied.
     * 
     * @throws Exception
     */
    @Test
    public void shouldUpdateSuperhero() throws Exception {
        byte[] heroJson = toJson(new Hero("Company Man"));
        MvcResult results = invokeCreateHero(heroJson)
                .andExpect(status().isCreated())
                .andReturn();
        Hero hero = fromJsonResult(results, Hero.class);

        final String newName = "Salary Man";
        invokeUpdateHero(hero.getId(), toJson(new Hero(newName))).andExpect(status().isNoContent());

        invokeGetHero(hero.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(newName)));
    }

    /**
     * Expect a bad request with error "Already Exists" if update a superhero to
     * name that already exists. applied.
     * 
     * @throws Exception
     */
    @Test
    public void shouldBadRequestUpdateSuperheroToExistingSuperhero() throws Exception {
        byte[] heroJson = toJson(new Hero("Company Man"));
        MvcResult results = invokeCreateHero(heroJson).andExpect(status().isCreated()).andReturn();
        Hero hero = fromJsonResult(results, Hero.class);

        final String newName = "Superman";
        invokeUpdateHero(hero.getId(), toJson(new Hero(newName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].entity", is(Hero.class.getName())))
                .andExpect(jsonPath("$.errors[0].property", is("name")))
                .andExpect(jsonPath("$.errors[0].message", is("Already Exists")));
        ;
    }

    /**
     * Create and then update a Hero and retrieve to make sure the update was
     * applied.
     * 
     * @throws Exception
     */
    @Test
    public void shouldBadRequestUpdateSuperheroWithBadName() throws Exception {
        byte[] heroJson = toJson(new Hero("Company Man"));
        MvcResult results = invokeCreateHero(heroJson).andExpect(status().isCreated()).andReturn();
        Hero hero = fromJsonResult(results, Hero.class);

        final String newName = null;
        results = invokeUpdateHero(hero.getId(), toJson(new Hero(newName)))
                .andExpect(status().isBadRequest())
                .andReturn();
        checkInvalidNameErrorResponse(results, "name is required");

        results = invokeUpdateHero(hero.getId(), toJson(new Hero("")))
                .andExpect(status().isBadRequest()).andReturn();
        checkInvalidNameErrorResponse(results, "name must be between 1 and 20 characters long");

    }

    private void checkInvalidNameErrorResponse(MvcResult results, String msg) throws Exception {
        ValidationError apiError = fromJsonResult(results, ValidationError.class);
        assertThat(apiError.getHttpStatus()).isIn(HttpStatus.BAD_REQUEST,
                HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(apiError.getErrors().size()).isEqualTo(1);

        ValidationError.Error error = apiError.getErrors().get(0);
        assertThat(error.getEntity()).isEqualTo("io.navan.heroesbackend.Hero");
        assertThat(error.getProperty()).isEqualTo("name");
        assertThat(error.getMessage()).isEqualToIgnoringCase(msg);

        LOG.debug("ApiError\n" + apiError.toString());
    }

    /**
     * Updating a hero for an id that does not exist in database should return 404
     * not found.
     * 
     * @throws Exception
     */
    @Test
    public void should404UpdateNotfoundSuperhero() throws Exception {
        Hero[] heroes = getAllHeroes();
        assertThat(heroes.length).isEqualTo(1);

        // Only one superhero, so its id plus 1 must not exist...

        invokeUpdateHero(heroes[0].getId() + 1, toJson(new Hero("test")))
                .andExpect(status().isNotFound());
    }

    /**
     * Delete a superhero must well and truly delete it.
     * 
     * @throws Exception
     */
    @Test
    public void shouldDeleteSuperhero() throws Exception {
        Hero[] heroes = getAllHeroes();
        assertThat(heroes.length).isEqualTo(1);
        Long heroId = heroes[0].getId();

        // Successfully get it before deleting it.
        invokeGetHero(heroId).andExpect(status().isOk());

        // Delete it.
        invokeDeleteHero(heroId).andExpect(status().isNoContent());

        // List, previously of length 1, now empty
        assertThat(getAllHeroes().length).isEqualTo(0);

        // Now can no longer get the deleted Hero.
        invokeGetHero(heroId).andExpect(status().isNotFound());
    }

    /**
     * Deleting a hero for an id that does not exist in database should return HTTP
     * 404 - not found.
     * 
     * @throws Exception
     */
    @Test
    public void should404DeleteNotfoundSuperhero() throws Exception {
        Hero[] heroes = getAllHeroes();
        assertThat(heroes.length).isEqualTo(1);

        // Only one superhero, so its id plus 1 must not exist...

        invokeDeleteHero(heroes[0].getId() + 1).andExpect(status().isNotFound());
    }

    /**
     * Should get an internal server error when delete Superhero by non-numeric id.
     * 
     * @throws Exception
     */
    @Test
    public void shouldServerErrorDeleteSuperheroWithBadId() throws Exception {
        mvc.perform(delete(BASE_URL + "xxx")).andExpect(status().isNotFound());
    }

    /**
     * Should search by name (case-insensitive) and get expected results.
     * 
     * @throws Exception
     */
    @Test
    public void shouldSearchSuperheroes() throws Exception {
        String[] heroes = new String[] { "Supergirl", "Company Man", "Cat Lady", "Cat Girl",
                "Silly Putty Man" };
        for (String hero : heroes) {
            invokeCreateHero(toJson(new Hero(hero)));
        }

        invokeSearchHeroes("girl")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Cat Girl", "Supergirl")));
        invokeSearchHeroes("man")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name",
                        containsInAnyOrder("Superman", "Company Man", "Silly Putty Man")));
        invokeSearchHeroes("cat")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Cat Girl", "Cat Lady")));
        invokeSearchHeroes("cthulhu")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /*
     * Private utility functions used by tests
     */

    /**
     * Convert JSON Result to object.
     * 
     * @param result
     *            The contents
     * @param tClass
     *            The expected object class
     * @return The result as class.
     * 
     * @throws Exception
     *             if you got any of the above wrong.
     */
    <T> T fromJsonResult(MvcResult result, Class<T> tClass) throws Exception {
        return this.mapper.readValue(result.getResponse().getContentAsString(), tClass);
    }

    /**
     * Convert object to JSON bytes.
     * 
     * @param object
     *            The object to JSONify
     * @return byte array with JSON representation
     * @throws Exception
     */
    private byte[] toJson(Object object) throws Exception {
        return this.mapper.writeValueAsString(object).getBytes();
    }

    /**
     * Get all the heroes.
     * 
     * @return Heroes[]
     * @throws Exception
     */
    private Hero[] getAllHeroes() throws Exception {
        Hero[] heroes = fromJsonResult(invokeAllHeroes().andReturn(), Hero[].class);
        return heroes;
    }

    /*
     * Hit the endpoints...
     */

    private ResultActions invokeAllHeroes() throws Exception {
        return mvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions invokeSearchHeroes(String term) throws Exception {
        return mvc.perform(get(BASE_URL + "?name=" + term).accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions invokeGetHero(Long id) throws Exception {
        return mvc.perform(get(BASE_URL + id).accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions invokeCreateHero(byte[] heroJson) throws Exception {
        return mvc.perform(post(BASE_URL).content(heroJson).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions invokeUpdateHero(Long id, byte[] heroJson) throws Exception {
        return mvc.perform(
                put(BASE_URL + id).content(heroJson).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions invokeDeleteHero(Long id) throws Exception {
        return mvc.perform(delete(BASE_URL + id));
    }
}
