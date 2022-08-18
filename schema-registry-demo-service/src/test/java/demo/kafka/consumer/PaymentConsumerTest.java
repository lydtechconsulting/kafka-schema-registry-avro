package demo.kafka.consumer;

import demo.kafka.event.SendPayment;
import demo.kafka.service.PaymentService;
import demo.kafka.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PaymentConsumerTest {

    private PaymentService serviceMock;
    private PaymentConsumer consumer;

    @BeforeEach
    public void setUp() {
        serviceMock = mock(PaymentService.class);
        consumer = new PaymentConsumer(serviceMock);
    }

    /**
     * Ensure that the JSON message is successfully passed on to the service, having been correctly unmarshalled into its PoJO form.
     */
    @Test
    public void testListen_Success() {
        String key = "test-key";
        SendPayment command = TestData.buildSendPayment(randomUUID().toString());
        consumer.listen(key, command);
        verify(serviceMock, times(1)).process(key, command);
    }

    /**
     * If an exception is thrown, an error is logged but the processing completes successfully.
     *
     * This ensures the consumer offsets are updated so that the message is not redelivered.
     */
    @Test
    public void testListen_ServiceThrowsException() {
        String key = "test-key";
        SendPayment command = TestData.buildSendPayment(randomUUID().toString());
        doThrow(new RuntimeException("Service failure")).when(serviceMock).process(key, command);
        consumer.listen(key, command);
        verify(serviceMock, times(1)).process(key, command);
    }
}
