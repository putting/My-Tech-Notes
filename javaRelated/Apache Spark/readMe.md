# Apache Spark

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
