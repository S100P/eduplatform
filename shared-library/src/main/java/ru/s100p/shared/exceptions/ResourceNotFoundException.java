package ru.s100p.shared.exceptions;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier), 
              "RESOURCE_NOT_FOUND");
    }
}
