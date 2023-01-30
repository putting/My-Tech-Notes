# Kafka Questions

## Urls
https://www.interviewbit.com/kafka-interview-questions/

### Kafka
Contains many indepth questions. Sample:
- What do you mean by a Partition in Kafka? By default the records key is used.
- What is the maximum size of a message that Kafka can receive?
  By default, the maximum size of a Kafka message is 1MB (megabyte). The broker settings allow you to modify the size. 
  Kafka, on the other hand, is designed to handle 1KB messages as well
- What do you understand about a consumer group in Kafka?
- How do you monitor Kafka? Kafka does not have a complete set of monitoring tools.
  - Track System Resource Consumption: It can be used to keep track of system resources such as memory, CPU, and disk utilization over time.
  - Monitor threads and JVM usage: Kafka relies on the Java garbage collector to free up memory, ensuring that it runs frequently thereby guaranteeing that the Kafka cluster is more active.
  - Keep an eye on the broker, controller, and replication statistics so that the statuses of partitions and replicas can be modified as needed.
  - Finding out which applications are causing excessive demand and identifying performance bottlenecks might help solve performance issues rapidly.

### Kafka Streams
https://www.baeldung.com/java-kafka-streams-vs-kafka-consumer
- Important to understand that it has state stores (vs consumers which are stateless)
  - Stateless: filter, map, flatMap, ** (not sure)or groupBy**
  - Stateful: reduce, aggregation
  - KStream, KTable and GlobalKTable
