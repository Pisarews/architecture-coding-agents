package org.archagents.fhir.web;

public class UnsupportedResourceTypeException extends RuntimeException {

    public UnsupportedResourceTypeException(String resourceType) {
        super("Resource type '" + resourceType + "' is not supported");
    }
}
