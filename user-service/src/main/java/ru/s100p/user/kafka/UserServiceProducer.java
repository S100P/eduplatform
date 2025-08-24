package ru.s100p.user.kafka;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import ru.s100p.shared.events.UserProfileUpdatedEvent;
import ru.s100p.shared.events.UserRegisteredEvent;
import ru.s100p.user.entity.Role;
import ru.s100p.user.entity.User;
import ru.s100p.user.entity.UserRole;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ru.s100p.shared.constants.KafkaTopicNames.USER_PROFILE_UPDATED_TOPIC;
import static ru.s100p.shared.constants.KafkaTopicNames.USER_REGISTERED_TOPIC;

@Data
@Slf4j
@Component
public class UserServiceProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Публикация события о регистрации пользователя
     */
    public void publishUserRegistered(User user) {
        try {
            UserRegisteredEvent event = new UserRegisteredEvent();
            event.setUserId(user.getId());
            event.setEmail(user.getEmail());
            event.setFirstName(user.getFirstName());
            event.setLastName(user.getLastName());
            event.setRoles(user.getRoles().stream()
                    .filter(Objects::nonNull)
                    .map(UserRole::getRole)
                    .filter(Objects::nonNull)
                    .map(Role::getName)
                    .collect(Collectors.toUnmodifiableSet()));

            // Асинхронная отправка с обработкой результата
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(USER_REGISTERED_TOPIC, user.getId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Событие UserRegistered отправлено успешно: userId={}, eventId={}, offset={}",
                            user.getId(), event.getEventId(), result.getRecordMetadata().offset());
                } else {
                    log.error("Ошибка при отправке события UserRegistered: userId={}, eventId={}",
                            user.getId(), event.getEventId(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Критическая ошибка при создании события UserRegistered для userId={}", user.getId(), e);
        }
    }

    /**
     * Публикация события об обновлении профиля
     */
    public void publishUserProfileUpdated(User user, Map<String, Object> changes) {
        try {
            UserProfileUpdatedEvent event = UserProfileUpdatedEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .changes(changes)
                    .build();

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(USER_PROFILE_UPDATED_TOPIC, user.getId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ Событие UserProfileUpdated отправлено: userId={}, изменения={}",
                            user.getId(), changes.keySet());
                } else {
                    log.error("❌ Ошибка при отправке события UserProfileUpdated: userId={}",
                            user.getId(), ex);
                    handleEventPublishFailure(event, ex);
                }
            });

        } catch (Exception e) {
            log.error("Критическая ошибка при создании события UserProfileUpdated для userId={}", user.getId(), e);
        }
    }

    /**
     * Публикация события о деактивации пользователя
     */
    public void publishUserDeactivated(User user) {
        try {
            // Создаем кастомное событие для деактивации
            Map<String, Object> deactivationData = Map.of(
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "deactivatedAt", user.getUpdatedAt().toString(),
                    "eventType", "USER_DEACTIVATED"
            );

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send("user-deactivated-topic", user.getId().toString(), deactivationData);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ Событие UserDeactivated отправлено: userId={}", user.getId());
                } else {
                    log.error("❌ Ошибка при отправке события UserDeactivated: userId={}", user.getId(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Критическая ошибка при создании события UserDeactivated для userId={}", user.getId(), e);
        }
    }

    /**
     * Публикация события о смене роли
     */
    public void publishRoleChanged(Long userId, String roleName, String action) {
        try {
            Map<String, Object> roleChangeData = Map.of(
                    "userId", userId,
                    "roleName", roleName,
                    "action", action, // "ASSIGNED" или "REVOKED"
                    "eventType", "USER_ROLE_CHANGED"
            );

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send("user-role-changed-topic", userId.toString(), roleChangeData);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ Событие RoleChanged отправлено: userId={}, role={}, action={}",
                            userId, roleName, action);
                } else {
                    log.error("❌ Ошибка при отправке события RoleChanged: userId={}", userId, ex);
                }
            });

        } catch (Exception e) {
            log.error("Критическая ошибка при создании события RoleChanged для userId={}", userId, e);
        }
    }

    /**
     * Публикация события о верификации email
     */
    public void publishEmailVerified(Long userId, String email) {
        try {
            Map<String, Object> verificationData = Map.of(
                    "userId", userId,
                    "email", email,
                    "verifiedAt", java.time.LocalDateTime.now().toString(),
                    "eventType", "EMAIL_VERIFIED"
            );

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send("email-verified-topic", userId.toString(), verificationData);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ Событие EmailVerified отправлено: userId={}, email={}", userId, email);
                } else {
                    log.error("❌ Ошибка при отправке события EmailVerified: userId={}", userId, ex);
                }
            });

        } catch (Exception e) {
            log.error("Критическая ошибка при создании события EmailVerified для userId={}", userId, e);
        }
    }

    /**
     * Публикация события о смене пароля
     */
    public void publishPasswordChanged(Long userId) {
        try {
            Map<String, Object> passwordChangeData = Map.of(
                    "userId", userId,
                    "changedAt", java.time.LocalDateTime.now().toString(),
                    "eventType", "PASSWORD_CHANGED"
            );

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send("password-changed-topic", userId.toString(), passwordChangeData);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ Событие PasswordChanged отправлено: userId={}", userId);
                } else {
                    log.error("❌ Ошибка при отправке события PasswordChanged: userId={}", userId, ex);
                }
            });

        } catch (Exception e) {
            log.error("Критическая ошибка при создании события PasswordChanged для userId={}", userId, e);
        }
    }

    // ===== Вспомогательные методы =====

    /**
     * Извлечение названий ролей из пользователя
     */
    private java.util.Set<String> extractRoleNames(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return java.util.Set.of("STUDENT"); // Роль по умолчанию
        }

        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Обработка ошибок публикации событий
     * В продакшене можно сохранять неотправленные события в БД для повторной отправки
     */
    private void handleEventPublishFailure(Object event, Throwable ex) {
        // TODO: Реализовать сохранение в таблицу failed_events для retry механизма
        log.error("Сохранение события для повторной отправки: {}", event);

        // Можно отправить алерт в мониторинг
        // alertService.sendAlert("Kafka event publish failed", ex);
    }


}
