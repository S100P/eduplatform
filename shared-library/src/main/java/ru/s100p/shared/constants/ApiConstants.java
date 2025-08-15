package ru.s100p.shared.constants;

public final class ApiConstants {
    
    private ApiConstants() {}
    
    // API versions
    public static final String API_V1 = "/api/v1";
    
    // Common endpoints
    public static final String USERS_ENDPOINT = "/users";
    public static final String COURSES_ENDPOINT = "/courses";
    public static final String ENROLLMENTS_ENDPOINT = "/enrollments";
    public static final String PAYMENTS_ENDPOINT = "/payments";
    
    // HTTP Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String CONTENT_TYPE_JSON = "application/json";
    
    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}