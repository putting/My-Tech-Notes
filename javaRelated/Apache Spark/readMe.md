# Apache Spark

- RDD (Resilient Distributed Dataset) 
        [spark tutorials](https://www.tutorialspoint.com/apache_spark/apache_spark_rdd.htm)
         A fundamental data structure of Spark. It is an immutable distributed collection of objects. Each dataset in RDD is divided into          logical partitions, which may be computed on different nodes of the cluster. RDDs can contain any type of Python, Java, or                Scala objects, including user-defined classes.
         

## Spark SQL
[Spark SQL Docs](https://spark.apache.org/docs/latest/sql-programming-guide.html#overview)
Spark SQL is a Spark module for structured data processing. Unlike the basic **Spark RDD API**, the interfaces provided by Spark SQL provide Spark with more information about the structure of both the data and the computation being performed.

- Dataset is a distributed collection of data. Benefits of RDD but with SQL engine.
- DataFrame is a Dataset organized into named columns

## Config
- Local only: spark-master: local[*]

## Create a Session

```java
        SparkConf sparkConf = new SparkConf();
        SparkSession.Builder builder = SparkSession.builder().appName("System")
                .master(configuration.getSparkMaster())
                .config(sparkConf);

        if (configuration.getSparkUiPort() > 0) {
            builder = builder.config("spark.ui.port", configuration.getSparkUiPort());
        } else {
            builder = builder.config("spark.ui.enabled", false);
        }
        SparkSession session = builder.getOrCreate();
        Path sparkCheckpoint = Files.createTempDirectory("spark_checkpoint");
        FileUtils.forceDeleteOnExit(sparkCheckpoint.toFile());
        session.sparkContext().setCheckpointDir(sparkCheckpoint.toAbsolutePath().toString());  
```
