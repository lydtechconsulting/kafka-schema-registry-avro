package demo.kafka.controller;

import demo.kafka.rest.api.SendPaymentRequest;
import demo.kafka.service.PaymentService;
import demo.kafka.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PaymentControllerTest {

    private PaymentService serviceMock;
    private PaymentController controller;

    @BeforeEach
    public void setUp() {
        serviceMock = mock(PaymentService.class);
        controller = new PaymentController(serviceMock);
    }

    /**
     * Ensure that the REST request is successfully passed on to the service.
     */
    @Test
    public void testListen_Success() {
        SendPaymentRequest request = TestData.buildSendPaymentRequest(randomUUID().toString());
        ResponseEntity response = controller.sendPayment(request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), equalTo(request.getPaymentId()));
        verify(serviceMock, times(1)).process(request.getPaymentId(), request);
    }

    /**
     * If an exception is thrown, an error is logged but the processing completes successfully.
     *
     * This ensures the consumer offsets are updated so that the message is not redelivered.
     */
    @Test
    public void testListen_ServiceThrowsException() {
        SendPaymentRequest request = TestData.buildSendPaymentRequest(randomUUID().toString());
        doThrow(new RuntimeException("Service failure")).when(serviceMock).process(request.getPaymentId(), request);
        ResponseEntity response = controller.sendPayment(request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.getBody(), equalTo(request.getPaymentId()));
        verify(serviceMock, times(1)).process(request.getPaymentId(), request);
    }
}
