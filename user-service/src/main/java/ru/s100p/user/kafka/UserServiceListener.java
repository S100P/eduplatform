package ru.s100p.user.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.s100p.shared.events.PaymentProcessedEvent;

import static ru.s100p.shared.constants.KafkaGroupNames.USER_SERVICE_GROUP;
import static ru.s100p.shared.constants.KafkaTopicNames.PAYMENT_PROCESSED_TOPIC;

@Component
@EnableKafka
@RequiredArgsConstructor
public class UserServiceListener {

    // Consumer для получения событий от других сервисов (если нужно)
    @KafkaListener(topics = PAYMENT_PROCESSED_TOPIC, groupId = USER_SERVICE_GROUP)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        // Обновить статус пользователя при успешной оплате
}
}
