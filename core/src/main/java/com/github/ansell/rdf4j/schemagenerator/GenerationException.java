package com.github.ansell.rdf4j.schemagenerator;

/**
 * @author Jakob Frank (jakob@apache.org)
 */
public class GenerationException extends Exception {

    public GenerationException() {
        super();
    }

    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenerationException(Throwable cause) {
        super(cause);
    }
}
