package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends BaseEvent {
    private Long paymentId;
    private Long userId;
    private Long courseId;
    private BigDecimal amount;
    private String failureReason;
    private String errorCode;
    
    public PaymentFailedEvent() {
        super();
        setEventType("PAYMENT_FAILED");
    }
}