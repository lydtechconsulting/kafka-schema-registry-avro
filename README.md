# Kafka Spring Boot Schema Registry & Avro Demo Project

Spring Boot application demonstrating usage of the Kafka Schema Registry with Avro serialisation.

## Build
```
mvn clean install
```

## Run Spring Boot Application

### Run docker containers

From root dir run the following to start dockerised Kafka, Zookeeper, Schema Registry, and Control Center:
```
docker-compose up -d
```

### Start demo spring boot application
```
cd schema-registry-demo-service/

java -jar target/schema-registry-demo-service-1.0.0.jar
```

### Register schemas:

From `avro-schema` project dir:
```
mvn schema-registry:register
```
Output:
```
[INFO] --- kafka-schema-registry-maven-plugin:5.5.5:register (default-cli) @ avro-schema ---
[INFO] Registered subject(payment-sent-value) with id 1 version 1
[INFO] Registered subject(send-payment-value) with id 2 version 1
```

### Schema Registry API curl:

List subjects:
```
curl -X GET http://localhost:8081/subjects
```

Get registered schemas for given Ids:
```
curl -X GET http://localhost:8081/schemas/ids/1
```
```
curl -X GET http://localhost:8081/schemas/ids/2
```

### Produce a send-payment command event:

Jump onto Schema Registry docker container:
```
docker exec -ti schema-registry bash
```

Produce send-payment command event:
```
kafka-avro-console-producer \
--topic send-payment \
--broker-list kafka:29092 \
--property schema.registry.url=http://localhost:8081 \
--property value.schema.id=2 \
--property key.schema='{"type":"string"}' \
--property "key.separator=:" \
--property parse.key=true \ 
```
Now enter the event (with key prefix):
```
"0e8a9a5f-1d4f-46bc-be95-efc6af8fb308":{"payment_id": "0e8a9a5f-1d4f-46bc-be95-efc6af8fb308", "amount": 3.0, "currency": "USD", "to_account": "toAcc", "from_account": "fromAcc"}
```

The send-payment command event is consumed by the application, which emits a resulting payment-sent event.

Check for the payment-sent event:
```
kafka-avro-console-consumer \
--topic payment-sent \
--property schema.registry.url=http://localhost:8081 \
--bootstrap-server kafka:29092 \
--from-beginning
```
Output:
```
{"payment_id":"0e8a9a5f-1d4f-46bc-be95-efc6af8fb308","amount":3.0,"currency":"USD","to_account":"toAcc","from_account":"fromAcc"}
```

### Kafka Confluent Control Center

Confluent Control Center is a UI over the Kafka cluster, providing configuration, data and information on the brokers, topics and messages.  It integrates with Schema Registry, enabling viewing of schemas.

Navigate to the Control Center:
```
http://localhost:9021
```

Select 'Topics' / 'send-payment' / 'Schemas' to view the schema for this message.

### List topics:

Jump on to Kafka docker container:
```
docker exec -ti kafka bash
```

List topics:
```
kafka-topics --list --bootstrap-server localhost:9092
```

View schemas:
```
kafka-console-consumer \
--topic _schemas \
--bootstrap-server kafka:29092 \
--from-beginning
```

## Avro

Generate the source code for the events using the Avro schema: (optional - this happens as part of the `install`)
```
mvn clean generate-sources
```

## Testing

### Integration Tests

Run integration tests with `mvn clean test`

The tests demonstrate:

- calling the REST endpoint to trigger sending a payment, with a resulting payment-sent event being emitted.  
- sending a send-payment command event to the inbound Kafka topic which is consumed by the application triggering sending a payment, with a resulting payment-sent event being emitted.

The tests first add stub mappings to the Schema Registry wiremock that satisfy the calls from the Kafka Avro serialisers enabling them to perform their serialisation.

### Component Tests

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

Run tests (from parent directory or `component-test` directory):
```
cd ../component-test

mvn test -Pcomponent
```

Run tests leaving containers up:
```
mvn test -Pcomponent -Dcontainers.stayup
```

#### Kafka Confluent Control Center

To view the Control Center UI, first enable this setting in the `pom.xml`:
```
<kafka.control.center.enabled>true</kafka.control.center.enabled>
```

To view data flowing through the system, enable the `PaymentEndToEndComponentTest.testThrottledSend()` which is `@Disabled` by default.

Leave the test containers up following a test run, and obtain the mapped docker port via:
```
docker ps
```

For example, the mapped port in this case is `52853`:
```
47140a515c3c confluentinc/cp-enterprise-control-center:6.2.4  [...] 0.0.0.0:52853->9021/tcp ct-kafka-control-center
```

Use this to navigate to the Control Center:
```
http://localhost:52853
```

#### Conduktor Platform Console

To view the Conduktor Platform Console UI, first enable this setting in the `pom.xml`:
```
<conduktor.enabled>true</conduktor.enabled>
```

To view data flowing through the system, enable the `PaymentEndToEndComponentTest.testThrottledSend()` which is `@Disabled` by default.

Leave the test containers up following a test run, and navigate to the Console at:
```
http://localhost:8088
```

The port can be overridden if required in the `pom.xml`, as it must be available on the local machine:
```
<conduktor.port>1234</conduktor.port>
```

#### Docker clean up

Manual clean up (if left containers up):
```
docker rm -f $(docker ps -aq)
```

Further docker clean up if network issues:
```
docker network prune
```
