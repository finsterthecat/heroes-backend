package io.navan.system;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.RollbackException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.NestedServletException;

import io.navan.heroesbackend.Hero;
import io.navan.heroesbackend.HeroController;

@ControllerAdvice(basePackageClasses = HeroController.class)
public class ControllerExceptionHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);
    
    /**
     * ConstraintViolations are raised when Entity validation annotations get violated. Capture all the
     * validation errors in ApiError and raise HTTP error BAD_REQUEST.
     * 
     * @param ex The exception
     * @param request The Web Request
     * @return ResponseEntity<ApiError> that will get returned in the response content
     */
    @ExceptionHandler({ ConstraintViolationException.class })
    public ResponseEntity<ValidationError> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        List<ValidationError.Error> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(new ValidationError.Error(violation.getRootBeanClass().getName(),
                    violation.getPropertyPath().toString(),
                    violation.getMessage()));
        }

        ValidationError apiError = new ValidationError(HttpStatus.BAD_REQUEST, "Validation Errors", errors);
        return new ResponseEntity<ValidationError>(apiError, new HttpHeaders(), apiError.getHttpStatus());
    }
    
    /**
     * Handle a DataIntegrityViolationException, most likely a 23505 sqlstate representing a resource already exists.
     * @param ex The DataIntegrityViolationException
     * @param request
     * @return
     */
    @ExceptionHandler({ DataIntegrityViolationException.class })
    public ResponseEntity<ValidationError> handleConstraintViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        LOG.debug("DataIntegrityViolation: " + ex.getLocalizedMessage(), ex);
        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            org.hibernate.exception.ConstraintViolationException cve =
                    (org.hibernate.exception.ConstraintViolationException)ex.getCause();
            String sqlstate = cve.getSQLState();

            if (sqlstate.equals("23505")) { //23505 is a unique key violation
                ValidationError apiError = new ValidationError(HttpStatus.BAD_REQUEST,
                        cve.getSQLException().getMessage(),
                        new ValidationError.Error(Hero.class.getName(), "name", "Already Exists"));
                return new ResponseEntity<ValidationError>(apiError,
                        new HttpHeaders(),
                        apiError.getHttpStatus());            
            } else {
                ValidationError apiError = new ValidationError(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unexpected SQLState: " + sqlstate + ".\n" + cve.getMessage(),
                        new ArrayList<>());
                return new ResponseEntity<ValidationError>(apiError,
                        new HttpHeaders(),
                        apiError.getHttpStatus());            
            }
        }
        ValidationError apiError = new ValidationError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected DataIntegrityViolation.\n" + ex.getMessage(),
                new ArrayList<>());
        return new ResponseEntity<ValidationError>(apiError,
                new HttpHeaders(),
                apiError.getHttpStatus());            
    }
    

    /**
     * Resource not found exception gets mapped to Http NOT_FOUND
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler({ ResourceNotFoundException.class })
    public ResponseEntity<ValidationError> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        ValidationError apiError = new ValidationError(HttpStatus.NOT_FOUND, "Resource not found",
                new ValidationError.Error(Hero.class.getName(), "*", "Hero not found"));
        return new ResponseEntity<ValidationError>(apiError, new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    /**
     * Fall through to handle all the other expected errors.
     * Tries to dig out the Constraint Violation exceptions that are sometimes
     * buried beneath TransactionSystemException, RollbackException, or NestedServletException.
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler({ Exception.class })
    public ResponseEntity<ValidationError> handleAll(Exception ex, WebRequest request) {
        //
        // Dig out any ConstraintViolationExceptions (gold!), which are quite often,
        // buried beneath varied containing transactions (dross!)
        if (ex instanceof TransactionSystemException ||
                ex instanceof RollbackException ||
                ex instanceof NestedServletException) {
            if (null != ex.getCause()) {
                if (ex.getCause() instanceof ConstraintViolationException) {
                    return handleConstraintViolation((ConstraintViolationException)ex.getCause(), request);
                } else if (ex.getCause() instanceof Exception) {
                    return handleAll((Exception)ex.getCause(), request);        //try again...
                }
            }
        }

        LOG.debug("Unexpected error: {}" + ex.toString(), ex);
        
        ValidationError apiError = new ValidationError(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage(),
                new ValidationError.Error("unknown.class", "unknown.property", "System error"));
        return new ResponseEntity<ValidationError>(apiError, new HttpHeaders(), apiError.getHttpStatus());
    }

}
