package io.rappt.swagger;

/**
 * An exception whilst attempting to process Swagger YAML.
 */
public class SwaggerException extends Exception {
    public SwaggerException() {
        super();
    }

    public SwaggerException(String message) {
        super(message);
    }

    public SwaggerException(String message, Throwable cause) {
        super(message, cause);
    }
}
