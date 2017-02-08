##4 Kafka

### 4.1 Kafka, Spark and Avro - Part 1, Kafka 101

- [website 3 part ](http://aseigneurin.github.io/2016/03/02/kafka-spark-avro-kafka-101.html) 
- [github kafka sandbox](https://github.com/aseigneurin/kafka-sandbox)
- **Author:** aseigneurin
- **Summary:** Nice simple intro to Kafla producer and Spark Consumer. Recomends **Apache Avro**.
                - Avro as a decoupling mechanism for DTO's allowing evolvable code.
                - `JavaPairInputDStream<String, String>` for kafka stream.
- **How to run the egs on windows:**
  - Problems Fixed
    I had to update the kafa-run.class.bat to reset classpath on my machine as picked up junit conflicts

  - Get Zookeeper running first
  cd C:\Apps\dev\tools\zookeeper-3.4.8\bin\
  zkserver

  - Run Kafka
  cd C:\Apps\dev\tools\kafka_2.11-0.10.1.0\bin\windows
  kafka-server-start.bat ../../config/server.properties

  - Create topics
  Althougn running the Producer-Consumer code appears to set up correctly
  kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --toipc paultopic

  alter a topic to have 2 partitions
  kafka-topics.bat --zookeeper localhost:2181 --alter --partitions 2 --topic mytopic


### 4.2 SIMPLE APACHE AVRO EXAMPLE USING JAVA
- [website](http://bigdatums.net/2016/01/20/simple-apache-avro-example-using-java/) 
- [github](https://github.com/nsonntag/big-datums/tree/master/programming/java/big-datums/src/main/java/com/bigdatums)
- **Author:** Big Datums
- **Summary:** Simple eg of how a domain object can be evolved using Avro. Check out other source code too. A bunch of interview related stuff and hashing , uuid.
                - Shows how to serialise/de-serialise.
