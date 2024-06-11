package demo.kafka.consumer;

import java.util.concurrent.atomic.AtomicInteger;

import demo.kafka.event.SendPayment;
import demo.kafka.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentConsumer {

    final AtomicInteger counter = new AtomicInteger();
    final PaymentService demoService;

    @KafkaListener(topics = "send-payment", groupId = "demo-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload final SendPayment command) {
        counter.getAndIncrement();
        log.debug("Received message [" +counter.get()+ "] - key: " + key);
        try {
            demoService.process(key, command);
        } catch (Exception e) {
            log.error("Error processing message: " + e.getMessage());
        }
    }
}

