package io.navan.system;

import static org.junit.Assert.*;

import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.navan.heroesbackend.Hero;

public class ControllerExceptionHandlerTests {

    
    private static final Logger LOG = LoggerFactory
            .getLogger(ControllerExceptionHandlerTests.class);
    
    //Example index violation messages received from embedded h2 database
    //These will differ with database vendor and will need to be accommodated through use of profiles
    //We expect different responses from different databases.
    static final String INDEX_VIOLATION_MESSAGE_PREFIX = "Unique index or primary key violation: ";
    //Message for duplicate hero
    static final String UNIQUE_HERO_INDEX_VIOLATION_CONSTRAINT_NAME = "Unique index or primary key violation: " +
            "\\\"UKSO1NO3WXP67IE8CS0EWTDSRBO_INDEX_2 ON PUBLIC.HERO(NAME) VALUES ('Duplicate Name', 9)\\\"; SQL statement:" +
            "\ninsert into hero (id, name) values (null, ?) [23505-196]";
    //Message for duplicate entity not hero
    static final String UNIQUE_NONHERO_INDEX_VIOLATION_CONSTRAINT_NAME = "Unique index or primary key violation: " +
            "\\\"UKSO1NO3WXP67IE8CS0EWTDSRBO_INDEX_2 ON PUBLIC.NOTHERO(NAME) VALUES ('Duplicate Name', 9)\\\"; SQL statement:" +
            "\ninsert into hero (id, name) values (null, ?) [23505-196]";
    
    /**
     * Should handle data integrity violations for hero already exists as BAD_REQUESTs
     */
    @Test
    public void testDataIntegrityViolationHeroAlreadyExists() {
        ControllerExceptionHandler ceh = new ControllerExceptionHandler();
        ResponseEntity<ValidationError> re =
                ceh.handleDataIntegrityViolation(new DataIntegrityViolationException(null, 
                        new ConstraintViolationException("bad stuff",
                                new SQLException(INDEX_VIOLATION_MESSAGE_PREFIX + "stuff not checked by the exception handler", "23505", 100),
                        "select whatever from something", UNIQUE_HERO_INDEX_VIOLATION_CONSTRAINT_NAME)
                        ), null);
        ValidationError ve = re.getBody();
        LOG.debug("testConstraintViolationHeroAlreadyExists: {}", ve);
        assertThat("Status is bad request", ve.getHttpStatus(), is(HttpStatus.BAD_REQUEST));
        assertThat("One Error", ve.getErrors(), hasSize(1));
        assertThat("Error is Already Exists", ve.getErrors().get(0).getMessage(), is("Already Exists"));
        assertThat("Entity is hero", ve.getErrors().get(0).getEntity(), is(Hero.class.getName()));
    }

    /**
     * Should handle data integrity violations that are not already exists errors as SYSTEM ERRORs since they are unexpected.
     */
    @Test
    public void testDataIntegrityViolationHeroUnexpectedError() {
        ControllerExceptionHandler ceh = new ControllerExceptionHandler();
        ResponseEntity<ValidationError> re =
                ceh.handleDataIntegrityViolation(new DataIntegrityViolationException(null, 
                        new ConstraintViolationException("bad stuff",
                                new SQLException(INDEX_VIOLATION_MESSAGE_PREFIX + "stuff not checked by the exception handler", "999", 100),
                        "select whatever from something", UNIQUE_HERO_INDEX_VIOLATION_CONSTRAINT_NAME)
                        ), null);
        ValidationError ve = re.getBody();
        LOG.debug("testConstraintViolationHeroOtherError: {}", ve);
        assertThat("Status is bad request", ve.getHttpStatus(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat("No Error Lines", ve.getErrors(), hasSize(0));
    }

    /**
     * Should handle data integrity violations for entities other than hero as BAD_REQUESTS but for unknown entities, not Hero.
     */
    @Test
    public void testDataIntegrityViolationOtherEntityAlreadyExists() {
        ControllerExceptionHandler ceh = new ControllerExceptionHandler();
        ResponseEntity<ValidationError> re =
                ceh.handleDataIntegrityViolation(new DataIntegrityViolationException(null, 
                        new ConstraintViolationException("bad stuff",
                                new SQLException(INDEX_VIOLATION_MESSAGE_PREFIX + "stuff not checked by the exception handler", "23505", 100),
                                "select whatever from something", UNIQUE_NONHERO_INDEX_VIOLATION_CONSTRAINT_NAME)
                        ), null);
        ValidationError ve = re.getBody();
        LOG.debug("testConstraintViolationHeroOtherError: {}", ve);
        assertThat("Status is bad request", ve.getHttpStatus(), is(HttpStatus.BAD_REQUEST));
        assertThat("One Error", ve.getErrors(), hasSize(1));
        assertThat("Error is Already Exists", ve.getErrors().get(0).getMessage(), is("Already Exists"));
        assertThat("Entity is unknown class", ve.getErrors().get(0).getEntity(), is(("unknown.class")));
    }

}
