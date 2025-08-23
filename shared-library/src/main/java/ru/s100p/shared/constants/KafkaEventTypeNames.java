package ru.s100p.shared.constants;

public final class KafkaEventTypeNames {
    private KafkaEventTypeNames() {}

    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    public static final String COURSE_CREATED = "COURSE_CREATED";
    public static final String COURSE_UPDATED = "COURSE_UPDATED";
    public static final String ENROLLMENT_CREATED = "ENROLLMENT_CREATED";
    public static final String LESSON_COMPLETED = "LESSON_COMPLETED";
    public static final String PAYMENT_PROCESSED = "PAYMENT_PROCESSED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String NOTIFICATION_REQUESTED = "NOTIFICATION_REQUESTED";
}
