package demo.kafka.service;

import demo.kafka.event.PaymentSent;
import demo.kafka.event.SendPayment;
import demo.kafka.lib.KafkaClient;
import demo.kafka.rest.api.SendPaymentRequest;
import demo.kafka.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PaymentServiceTest {

    private KafkaClient mockKafkaClient;
    private PaymentService service;

    @BeforeEach
    public void setUp() {
        mockKafkaClient = mock(KafkaClient.class);
        service = new PaymentService(mockKafkaClient);
    }

    @Test
    public void testProcess_ViaKafka() {
        String key = "test-key";
        SendPayment command = TestData.buildSendPayment(randomUUID().toString());
        service.process(key, command);
        verify(mockKafkaClient, times(1)).sendMessage(eq(key), any(PaymentSent.class));
    }

    @Test
    public void testProcess_ViaRest() {
        String key = "test-key";
        SendPaymentRequest command = TestData.buildSendPaymentRequest(randomUUID().toString());
        service.process(key, command);
        verify(mockKafkaClient, times(1)).sendMessage(eq(key), any(PaymentSent.class));
    }
}
