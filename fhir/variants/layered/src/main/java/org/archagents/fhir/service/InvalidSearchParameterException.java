package org.archagents.fhir.service;

public class InvalidSearchParameterException extends RuntimeException {

    public InvalidSearchParameterException(String parameter, String value) {
        super("Invalid value '" + value + "' for search parameter '" + parameter + "'");
    }
}
