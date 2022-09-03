# Kafka Spring Boot Schema Registry With Avro Demo Project

Spring Boot application demonstrating usage of the Kafka Schema Registry with Avro serialisation.

## Build
```
mvn clean install
```

## Run Spring Boot Application

### Run docker containers

From root dir run the following to start dockerised Kafka, Zookeeper, and Schema Registry:
```
docker-compose up -d
```

### Start demo spring boot application
```
cd schema-registry-demo-service/

java -jar target/schema-registry-demo-service-1.0.0.jar
```

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

## Avro

Generate the source code for the events using the Avro schema (optional - this happens as part of the `install`):
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

Manual clean up (if left containers up):
```
docker rm -f $(docker ps -aq)
```

Further docker clean up if network issues:
```
docker network prune
```
