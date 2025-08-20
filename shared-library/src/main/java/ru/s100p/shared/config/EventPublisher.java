package ru.s100p.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import ru.s100p.shared.events.BaseEvent;

// EventPublisher.java - сервис для публикации событий
@Service
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void publishUserEvent(String topic, BaseEvent event) {
        try {
            // Установка метаданных
            event.setSourceService("user-service");
            event.setCorrelationId(MDC.get("correlationId"));

            // Отправка с callback
            ListenableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, event.getEventId(), event);

            future.addCallback(
                    result -> log.info("Event sent successfully: {} to topic: {}",
                            event.getEventType(), topic),
                    failure -> log.error("Failed to send event: {} to topic: {}. Error: {}",
                            event.getEventType(), topic, failure.getMessage())
            );

        } catch (Exception e) {
            log.error("Error publishing event to topic {}: {}", topic, e.getMessage(), e);
            throw new EventPublishException("Failed to publish event", e);
        }
    }

    public void publishCourseEvent(String topic, BaseEvent event) {
        event.setSourceService("course-service");
        publishEvent(topic, event);
    }

    public void publishEnrollmentEvent(String topic, BaseEvent event) {
        event.setSourceService("enrollment-service");
        publishEvent(topic, event);
    }

    public void publishPaymentEvent(String topic, BaseEvent event) {
        event.setSourceService("payment-service");
        publishEvent(topic, event);
    }

    private void publishEvent(String topic, BaseEvent event) {
        try {
            event.setCorrelationId(MDC.get("correlationId"));

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, event.getEventId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event sent: {} to topic: {} with offset: {}",
                            event.getEventType(), topic, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send event: {} to topic: {}. Error: {}",
                            event.getEventType(), topic, ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Error publishing event to topic {}: {}", topic, e.getMessage(), e);
            throw new EventPublishException("Failed to publish event", e);
        }
    }
}
