package org.archagents.fhir.service;

public class UnsupportedSearchParameterException extends RuntimeException {

    public UnsupportedSearchParameterException(String resourceType, String parameter) {
        super("Search parameter '" + parameter + "' is not supported for " + resourceType);
    }
}
