package demo.kafka.service;

import demo.kafka.event.PaymentSent;
import demo.kafka.event.SendPayment;
import demo.kafka.lib.KafkaClient;
import demo.kafka.rest.api.SendPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    @Autowired
    private final KafkaClient kafkaClient;

    /**
     * Called via Kafka command event.
     */
    public void process(String key, SendPayment command) {
        sendPayment();
        PaymentSent outboundEvent = PaymentSent.newBuilder()
                .setPaymentId(command.getPaymentId())
                .setAmount(command.getAmount())
                .setCurrency(command.getCurrency())
                .setFromAccount(command.getFromAccount())
                .setToAccount(command.getToAccount())
                .build();
        kafkaClient.sendMessage(key, outboundEvent);
    }

    /**
     * Called via REST request.
     */
    public void process(String key, SendPaymentRequest request) {
        sendPayment();
        PaymentSent outboundEvent = PaymentSent.newBuilder()
                .setPaymentId(request.getPaymentId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setFromAccount(request.getFromAccount())
                .setToAccount(request.getToAccount())
                .build();
        kafkaClient.sendMessage(key, outboundEvent);
    }

    private void sendPayment() {
        // No-op.  Simulates sending the payment.  e.g. a REST POST to an external rails.
    }
}
