package ru.s100p.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.s100p.notification.service.NotificationService;
import ru.s100p.shared.events.PaymentProcessedEvent;
import ru.s100p.shared.events.UserRegisteredEvent;

import static ru.s100p.shared.constants.KafkaGroupNames.NOTIFICATION_SERVICE_GROUP;
import static ru.s100p.shared.constants.KafkaTopicNames.PAYMENT_PROCESSED_TOPIC;
import static ru.s100p.shared.constants.KafkaTopicNames.USER_REGISTERED_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = USER_REGISTERED_TOPIC, groupId = NOTIFICATION_SERVICE_GROUP)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user registered event: {}", event.getEventId());

        // Отправить welcome email
        notificationService.sendWelcomeEmail(
                event.getEmail(),
                event.getFirstName()
        );
    }

    @KafkaListener(topics = PAYMENT_PROCESSED_TOPIC, groupId = NOTIFICATION_SERVICE_GROUP)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment processed event: {}", event.getEventId());

        // Отправить подтверждение оплаты
        notificationService.sendPaymentConfirmation(event);
    }
}