package ru.s100p.shared.constants;

public final class KafkaTopicNames {
    private KafkaTopicNames() {}

    public static final String USER_REGISTERED_TOPIC = "user_registered_topic";
    public static final String USER_PROFILE_UPDATED_TOPIC = "user_profile_updated_topic";
    public static final String COURSE_CREATED_TOPIC = "course_created_topic";
    public static final String COURSE_UPDATED_TOPIC = "course_updated_topic";
    public static final String PAYMENT_PROCESSED_TOPIC = "payment_processed_topic";
    public static final String NOTIFICATION_REQUESTED_TOPIC = "notification_requested_topic";
}
