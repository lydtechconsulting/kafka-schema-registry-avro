package demo.kafka.integration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.tomakehurst.wiremock.client.WireMock;
import demo.kafka.KafkaDemoConfiguration;
import demo.kafka.event.PaymentSent;
import demo.kafka.event.SendPayment;
import demo.kafka.rest.api.SendPaymentRequest;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaString;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static demo.kafka.util.TestData.buildSendPayment;
import static demo.kafka.util.TestData.buildSendPaymentRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { KafkaDemoConfiguration.class, TestKafkaDemoConfiguration.class } )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureWireMock(port=0) // 0 is dynamic port which binds to the "wiremock.server.port" property
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true, topics = { "payment-sent" })
public class PaymentIntegrationTest {

    final static String SEND_PAYMENT_TOPIC = "send-payment";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTestListener testReceiver;

    @Autowired
    private KafkaTemplate testKafkaTemplate;

    @Configuration
    static class TestConfig {

        @Bean
        public KafkaTestListener testReceiver() {
            return new KafkaTestListener();
        }
    }

    /**
     * Use this receiver to consume messages from the outbound topic.
     */
    public static class KafkaTestListener {
        AtomicInteger counter = new AtomicInteger(0);

        @KafkaListener(groupId = "PaymentIntegrationTest", topics = "payment-sent", containerFactory = "testKafkaListenerContainerFactory", autoStartup = "true")
        void receive(@Payload final PaymentSent payload) {
            log.debug("KafkaTestListener - Received payment with Id: " + payload.getPaymentId());
            counter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Wait until the partitions are assigned.
        registry.getListenerContainers().stream().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
        testReceiver.counter.set(0);

        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.resetAllScenarios();
        WireMock.resetToDefault();

        registerSchema(1, "send-payment", SendPayment.getClassSchema().toString());
        registerSchema(2, "payment-sent", PaymentSent.getClassSchema().toString());
    }

    /**
     * Register the schema derived from the avro generated class.
     *
     * @param schemaId the schema id to use
     * @param subject the subject of the schema
     * @param schema the schema JSON string
     */
    private void registerSchema(int schemaId, String subject, String schema) throws Exception {
        // Register the Avro schema.
        stubFor(post(urlPathMatching("/subjects/"+subject+"-value"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{\"id\":"+schemaId+"}")));

        // Get the registered schema.
        final SchemaString schemaString = new SchemaString(schema);
        stubFor(get(urlPathMatching("/schemas/ids/"+schemaId))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(schemaString.toJson())));
    }

    /**
     * Send in multiple 'send payment' requests via REST and ensure an outbound 'payment sent' event is emitted for each.
     */
    @Test
    public void testSendPaymentViaRest() {
        int totalMessages = 10;
        for (int i=0; i<totalMessages; i++) {
            String paymentId = UUID.randomUUID().toString();

            SendPaymentRequest request = buildSendPaymentRequest(paymentId);
            sendRequestViaRest(request);
        }

        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testReceiver.counter::get, equalTo(totalMessages));
    }

    /**
     * Send in multiple 'send payment' Kafka command events and ensure an outbound 'payment sent' event is emitted for each.
     */
    @Test
    public void testSendPaymentViaKafka() throws Exception {
        int totalMessages = 10;
        for (int i=0; i<totalMessages; i++) {
            String paymentId = UUID.randomUUID().toString();

            SendPayment command = buildSendPayment(paymentId);
            sendCommandViaKafka(command);
        }

        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testReceiver.counter::get, equalTo(totalMessages));
    }

    private void sendRequestViaRest(SendPaymentRequest request) {
        ResponseEntity<String> response = restTemplate.postForEntity("/v1/payments/send", request, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), equalTo(request.getPaymentId()));
    }

    private void sendCommandViaKafka(SendPayment command) throws Exception {
        final ProducerRecord<Long, String> record = new ProducerRecord(SEND_PAYMENT_TOPIC, null, command.getPaymentId(), command);
        testKafkaTemplate.send(record).get();
    }
}
