# Kafka

## Intro to Spring Kafa
https://www.baeldung.com/spring-kafka
http://kafka.apache.org/intro#intro_topics
- producerFactory to create Producers for KafkaTemplate. Which is mainly config.

## Key Value Pairs (or just payload)
Keys are used to determine the *partition within a log* to which a message get's appended to. While the value is the actual payload of the message.
So v important dfor scalability.

## In order to send/consume custom Java objects then Serdes need defining.
eg Using JsonSerializer. eg `Kafka.JsonSerdes.getRawSerde(Long.class, true);` for long from Kafka serde class.

### Producers
KafkaTemplate.send returnsListenableFuture<SendResult<String, String>>. However for more async purposes add a **callback**.

### Listeners
- consumerFactory creates a Listener on the approp topic. Again config driven with onMessage callback called.
The above url uses `@KafkaListener` annotations with specific topics. Better to inherit MessageListener with the consumerFactory config.
You could have multiple consumerFactories for diff topics or say adding a message filtering listener.

## Kafka Streams
https://www.baeldung.com/java-kafka-streams
https://www.baeldung.com/java-kafka-streams-vs-kafka-consumer

