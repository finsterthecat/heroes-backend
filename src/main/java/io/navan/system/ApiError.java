package io.navan.system;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;

public class ApiError {
    
    private HttpStatus status;
    private String message;
    private List<Error> errors;
 
    public ApiError() {};
    
    public ApiError(HttpStatus status, String message, List<Error> errors) {
        super();
        this.setStatus(status);
        this.setMessage(message);
        this.setErrors(errors);
    }
 
    public ApiError(HttpStatus status, String message, Error error) {
        super();
        this.setStatus(status);
        this.setMessage(message);
        setErrors(Arrays.asList(error));
    }

    public static class Error {
        public String getEntity() {
            return entity;
        }

        public void setEntity(String entity) {
            this.entity = entity;
        }

        public String getProperty() {
            return property;
        }

        public void setField(String field) {
            this.property = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        private String entity;
        private String property;
        private String message;
        
        public Error() {}
        
        public Error(String entity, String field, String message) {
            this.entity = entity;
            this.property = field;
            this.message = message;
        }
        
        @Override
        public String toString() {
            return String.format("%s(%s): %s", this.getEntity(), this.getProperty(), this.getMessage());
        }
    }
    
    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("{\nstatus: %s,\nmessage: %s,\nerrors: [", this.getStatus(), this.getMessage()));
        boolean first = true;
        for (ApiError.Error e: this.getErrors()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("\n\t" + e.toString());
            first = false;
        }
        sb.append("\n\t]\n}");
        return sb.toString();
    }
    
}
