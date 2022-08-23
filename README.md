# Kafka Spring Boot Schema Registry With Avro Demo Project

Spring Boot application demonstrating usage of the Kafka Schema Registry with Avro serialisation.

## Build & Test
```
mvn clean install
```
## Avro

Generate the events using the Avro schema (optional):
```
mvn clean generate-sources
```

## Integration Tests

Run integration tests with `mvn clean test`

The tests demonstrate:

- calling the REST endpoint to trigger sending a payment, with a resulting payment-sent event being emitted.  
- sending a send-payment command event to the inbound Kafka topic which is consumed by the application triggering sending a payment, with a resulting payment-sent event being emitted.

The tests first add stub mappings to the Schema Registry wiremock that satisfy the calls from the Kafka Avro serialisers enabling them to perform their serialisation.

## Component Tests

The tests demonstrate sending multiple send-payment command events to the inbound Kafka topic which is consumed by the application.  Each event triggers sending a payment, with a resulting payment-sent event being emitted to the outbound topic, which the test consumer receives.

The service itself is dockerised, and a dockerised Kafka broker and a dockerised Kafka Schema Registry are started by the component test framework.

The tests first register the Avro schemas for the events with the Schema Registry so that the Kafka Avro serialisers are able to perform their serialisation.

For more on the component tests see: https://github.com/lydtechconsulting/component-test-framework

Build Spring Boot application jar:
```
mvn clean install
```

Build Docker container:
```
cd schema-registry-demo-service

docker build -t ct/schema-registry-demo-service:latest .
```

Run tests (from parent directory or `component-test` directory)::
```
cd ../component-test

mvn test -Pcomponent
```

Run tests leaving containers up:
```
mvn test -Pcomponent -Dcontainers.stayup
```

Manual clean up (if left containers up):
```
docker rm -f $(docker ps -aq)
```
