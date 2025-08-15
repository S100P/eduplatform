package ru.s100p.shared.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PaymentProcessedEvent extends BaseEvent {
    private Long paymentId;
    private Long userId;
    private Long courseId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String transactionId;
    private String status;
    
    public PaymentProcessedEvent() {
        super();
        setEventType("PAYMENT_PROCESSED");
    }
}