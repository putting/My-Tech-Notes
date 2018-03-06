# Testing

Topics to be covered will focus on testing Apache Spark using Cucumber.

### PicoContainer : (http://picocontainer.com/introduction.html)
Handles DI. Creates a map of classes which can instantiate instances. Recommended for use with Cucumber.
E.G With Spark test
```java
private final SparkResource sparkResource = new SparkResource();
    private MutablePicoContainer pico;

    private final Set<Class<?>> classSet = new HashSet<>();

    @Override
    public void start() {
        try {
            sparkResource.before();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        pico = new PicoBuilder()
                .withCaching()
                .withLifecycle()
                .build();
        pico.addComponent(sparkResource.sparkSession());
        pico.addComponent(new RecordingStorageService());
        pico.addComponent(new DatabaselessUnitOfWork());
        pico.addComponent(TransactionService.class);
        classSet.forEach(pico::addComponent);
    }
```


## JUnit
- @Rule: Gets run before every test. You can set a Timeout, create a folder
- @ClassRle: Gets run before a suite of tests.
- Custom Rules: Allow you to create your own fn.
- External Resource: This is typically @Before & @After. Might be setting up a db or a Spark instance.
  This is like an external custom rule.
**EG Starting up Spark Session**
```java
public class SparkResource extends ExternalResource {

    private SparkSession _spark;
    private final TemporaryFolder _tempFile = new TemporaryFolder();

    public SparkSession sparkSession() {return _spark;}

    @Override
    public void before() throws IOException {
        _tempFile.create();
        _spark = SparkSession.builder().appName("Test").master("local[*]")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer") // This forces spark to use the Kryo serialiser as it is the fastest
                .config("spark.ui.enabled", false)
                .config("spark.sql.shuffle.partitions", 5) // Again, limiting the number of partitions improves performance
                .getOrCreate();
        _spark.sparkContext()
                .setCheckpointDir(_tempFile.newFolder().toPath().toAbsolutePath().toString());
    }
    @Override
    public void after() {
        //Clear up before closing spark
        _spark.sparkContext().cancelAllJobs();
        _spark.catalog().clearCache();
        JavaConversions.mapAsJavaMap(_spark.sparkContext().getPersistentRDDs()).forEach((o, rdd) -> rdd.unpersist(true));
        _spark.close();
        _tempFile.delete();
    }
}
```
