package demo.kafka.lib;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import demo.kafka.event.PaymentSent;
import demo.kafka.properties.KafkaDemoProperties;
import demo.kafka.util.TestData;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaClientTest {

    private KafkaDemoProperties propertiesMock;
    private KafkaTemplate kafkaTemplateMock;
    private KafkaClient kafkaClient;

    @BeforeEach
    public void setUp() {
        propertiesMock = mock(KafkaDemoProperties.class);
        kafkaTemplateMock = mock(KafkaTemplate.class);
        kafkaClient = new KafkaClient(propertiesMock, kafkaTemplateMock);
    }

    /**
     * Ensure the Kafka client is called to emit a message.
     */
    @Test
    public void testProcess_Success() throws Exception {
        String key = "test-key";
        String outboundEventId = randomUUID().toString();
        String topic = "test-outbound-topic";

        PaymentSent outboundEvent = TestData.buildPaymentSent(outboundEventId);
        ProducerRecord<String, PaymentSent> expectedRecord = new ProducerRecord<>(topic, key, outboundEvent);

        when(propertiesMock.getOutboundTopic()).thenReturn(topic);
        CompletableFuture futureResult = mock(CompletableFuture.class);
        SendResult expectedSendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic,0),0,0,0,Long.valueOf(0),0, 0);
        when(futureResult.get()).thenReturn(expectedSendResult);
        when(expectedSendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplateMock.send(any(ProducerRecord.class))).thenReturn(futureResult);

        SendResult result = kafkaClient.sendMessage(key, outboundEvent);

        verify(kafkaTemplateMock, times(1)).send(expectedRecord);
        assertThat(result, equalTo(expectedSendResult));
    }

    /**
     * Ensure that an exception thrown on the send is cleanly handled.
     */
    @Test
    public void testProcess_ExceptionOnSend() throws Exception {
        String key = "test-key";
        String outboundEventId = randomUUID().toString();
        String topic = "test-outbound-topic";

        PaymentSent outboundEvent = TestData.buildPaymentSent(outboundEventId);
        ProducerRecord<String, PaymentSent> expectedRecord = new ProducerRecord<>(topic, key, outboundEvent);

        when(propertiesMock.getOutboundTopic()).thenReturn(topic);
        CompletableFuture futureResult = mock(CompletableFuture.class);
        SendResult expectedSendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic,0),0,0,0,Long.valueOf(0),0, 0);
        when(futureResult.get()).thenReturn(expectedSendResult);
        when(expectedSendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplateMock.send(any(ProducerRecord.class))).thenReturn(futureResult);

        doThrow(new ExecutionException("Kafka send failure", new Exception("Failed"))).when(futureResult).get();

        Exception exception = assertThrows(RuntimeException.class, () -> {
                kafkaClient.sendMessage(key, outboundEvent);
        });

        verify(kafkaTemplateMock, times(1)).send(expectedRecord);
        assertThat(exception.getMessage(), equalTo("Error sending message to topic " + topic));
    }
}
