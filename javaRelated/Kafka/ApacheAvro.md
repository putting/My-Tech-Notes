# Apache Avro

Introduction here: [https://dzone.com/articles/introduction-apache-avro](https://dzone.com/articles/introduction-apache-avro)

Main Docs: [http://avro.apache.org/docs/current/](http://avro.apache.org/docs/current/)

Apache Avro is a popular data serialization format and is gaining more users, because many Hadoop-based tools natively support Avro for serialization and deserialization.

What is Avro?
  - Data serialization system
  - Uses JSON based schemas
  - Uses RPC calls to send data
  - Schema's sent during data exchange
  
Avro relies on schemas so as to provide efficient serialization of the data. The schema is written in JSON format and describes the fields and their types. There are 2 cases:
  - when serializing to a file, the schema is written to the file.
  - in RPC - such as between Kafka and Spark - both systems should know the schema prior to exchanging data, or they could exchange the schema during the connection handshake.  

A bunch of dzone articles
  - Convert CSV Data to Avro Data [https://dzone.com/articles/convert-csv-data-avro-data](https://dzone.com/articles/convert-csv-data-avro-data)
  - MapReduce: [https://dzone.com/articles/mapreduce-avro-data-files](https://dzone.com/articles/mapreduce-avro-data-files)
  - Avro's Built-In Sorting [https://dzone.com/articles/avros-built-sorting](https://dzone.com/articles/avros-built-sorting)
  - Secondary Sorting with Avro [https://dzone.com/articles/secondary-sorting-avro](https://dzone.com/articles/secondary-sorting-avro)
