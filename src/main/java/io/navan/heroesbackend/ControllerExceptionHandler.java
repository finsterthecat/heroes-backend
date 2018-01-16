package io.navan.heroesbackend;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.RollbackException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.util.NestedServletException;

import io.navan.system.ApiError;

@ControllerAdvice
public class ControllerExceptionHandler {

    /**
     * ConstraintViolations are raised when Entity validation annotations get violated. Capture all the
     * validation errors in ApiError and raise HTTP error BAD_REQUEST.
     * 
     * @param ex The exception
     * @param request The Web Request
     * @return ResponseEntity<ApiError> that will get returned in the response content
     */
    @ExceptionHandler({ ConstraintViolationException.class })
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<ApiError.Error> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(new ApiError.Error(violation.getRootBeanClass().getName(),
                    violation.getPropertyPath().toString(),
                    violation.getMessage()));
        }

        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
        return new ResponseEntity<ApiError>(apiError, new HttpHeaders(), apiError.getStatus());
    }
    
    /**
     * Resource not found exception gets mapped to Http NOT_FOUND
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler({ ResourceNotFoundException.class })
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ApiError apiError = new ApiError(
          HttpStatus.NOT_FOUND, ex.getLocalizedMessage(), new ApiError.Error("unknown.class", "unknown.property", "resource not found"));
        return new ResponseEntity<ApiError>(apiError, new HttpHeaders(), apiError.getStatus());
    }
    
    /**
     * Argument type mismatch exception. Results in a Http Bad Request.
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler({ MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        ApiError apiError = new ApiError(
          HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), new ApiError.Error("unknown.class", "unknown.property", "bad data"));
        return new ResponseEntity<ApiError>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    /**
     * Fall through to handle all the other expected errors. Tries to dig out the Constraint Violation exceptions that are sometimes
     * buried beneath TransactionSystemException, RollbackException, or NestedServletException.
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler({ Exception.class })
    public ResponseEntity<ApiError> handleAll(Exception ex, WebRequest request) {
        //
        //Dig out any ConstraintViolationExceptions (gold!), which are quite often, buried beneath varied containing transactions (dross!)
        if (ex instanceof TransactionSystemException || ex instanceof RollbackException || ex instanceof NestedServletException) {
            if (null != ex.getCause()) {
                if (ex.getCause() instanceof ConstraintViolationException) {
                    return handleConstraintViolation((ConstraintViolationException)ex.getCause(), request);
                } else {
                    return handleAll((Exception)ex.getCause(), request);        //try again...
                }
            }
        }

        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage(), new ApiError.Error("unknown.class", "unknown.property", "system error"));
        return new ResponseEntity<ApiError>(apiError, new HttpHeaders(), apiError.getStatus());
    }

}
