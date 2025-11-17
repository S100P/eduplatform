package ru.s100p.shared.constants;

// ErrorCodes.java - коды ошибок
public final class ErrorCodes {

    // Security related errors
    public static final String INVALID_TOKEN = "INVALID_TOKEN";

    private ErrorCodes() {}

    // User related errors
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    
    // Course related errors
    public static final String COURSE_NOT_FOUND = "COURSE_NOT_FOUND";
    public static final String COURSE_NOT_PUBLISHED = "COURSE_NOT_PUBLISHED";
    public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";
    
    // Payment related errors
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String PAYMENT_ALREADY_PROCESSED = "PAYMENT_ALREADY_PROCESSED";
}