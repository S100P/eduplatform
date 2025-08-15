package ru.s100p.shared.constants;

public final class SecurityConstants {
    
    private SecurityConstants() {}
    
    // JWT
    public static final String JWT_SECRET_KEY = "${jwt.secret:defaultSecretKey}";
    public static final long JWT_EXPIRATION_TIME = 86400000; // 24 hours
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = 604800000; // 7 days
    
    // Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_GUEST = "GUEST";
    
    // Permissions
    public static final String PERMISSION_READ = "READ";
    public static final String PERMISSION_WRITE = "WRITE";
    public static final String PERMISSION_DELETE = "DELETE";
    public static final String PERMISSION_ADMIN = "ADMIN";
}