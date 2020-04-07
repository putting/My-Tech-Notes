# Out of Memory Issues in Apache Spark

Use ful article for JVM GC in general

## V good article on how Spark uses memory and the difference in the JVM GC's
(http://labs.criteo.com/2018/01/spark-out-of-memory/)

## GC verbose logging
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution -XX:+UnlockExperimentalVMOptions -XX:G1LogLevel=finest -XX:+UnlockDiagnosticVMOptions -XX:+G1PrintRegionLivenessInfo
