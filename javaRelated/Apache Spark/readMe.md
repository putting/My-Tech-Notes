# Apache Spark
Spark not only supports ‘Map’ and ‘reduce’. It also supports SQL queries, Streaming data, Machine learning (ML), and Graph algorithms.

- RDD (Resilient Distributed Dataset) 
        [spark tutorials](https://www.tutorialspoint.com/apache_spark/apache_spark_rdd.htm)
        - A fundamental data structure of Spark. It is an immutable distributed collection of objects. Each dataset in RDD is divided into logical partitions, which may be computed on different nodes of the cluster. RDDs can contain any type of Python, Java, or                Scala objects, including user-defined classes.
         

## Spark SQL
[Spark SQL Docs](https://spark.apache.org/docs/latest/sql-programming-guide.html#overview)
Spark SQL is a Spark module for structured data processing. Unlike the basic **Spark RDD API**, the interfaces provided by Spark SQL provide Spark with more information about the structure of both the data and the computation being performed.

- SchemaRDD, which provides support for structured and semi-structured data.
- Dataset is a distributed collection of data. Benefits of RDD but with SQL engine. A Dataset can be constructed from JVM objects and then manipulated using functional transformations (map, flatMap, filter, etc.).
- DataFrame is a Dataset organized into named columns (conceptually equivalent to a table in a relational database).

### SQl notation using DataFrames
- df.printSchema(), df.select("name").show(), df.filter($"age" > 21).show(), df.groupBy("age").count().show()
- You can create views from Dfs: df.createOrReplaceTempView("viewName") and can be refered to as sql: 
        val sqlDF = spark.sql("SELECT * FROM viewName")

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


