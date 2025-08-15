package ru.s100p.shared.constants;

// BusinessConstants.java - бизнес константы
public final class BusinessConstants {
    
    private BusinessConstants() {}
    
    // Course statuses
    public static final String COURSE_STATUS_DRAFT = "DRAFT";
    public static final String COURSE_STATUS_PUBLISHED = "PUBLISHED";
    public static final String COURSE_STATUS_ARCHIVED = "ARCHIVED";
    
    // Enrollment statuses
    public static final String ENROLLMENT_STATUS_ACTIVE = "ACTIVE";
    public static final String ENROLLMENT_STATUS_COMPLETED = "COMPLETED";
    public static final String ENROLLMENT_STATUS_CANCELLED = "CANCELLED";
    
    // Payment statuses
    public static final String PAYMENT_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    public static final String PAYMENT_STATUS_FAILED = "FAILED";
    public static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";
    
    // Notification types
    public static final String NOTIFICATION_TYPE_EMAIL = "EMAIL";
    public static final String NOTIFICATION_TYPE_PUSH = "PUSH";
    public static final String NOTIFICATION_TYPE_SMS = "SMS";
}