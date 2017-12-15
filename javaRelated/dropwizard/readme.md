# Info

## DataSourceFactory

DW config defines data sources as DataSourceFactory and stored in config class
```java
public class AppConfiguration extends Configuration
    @NotNull
    private DataSourceFactory avroRegistryDatabase;

    @NotNull
    private DataSourceFactory ictsDatabase;
```    

## Guice Injectors for each of the Applications Modules

Each of the modules is related to the data source and associated Daos and services

```java
public class VaultConsumerInjectorBuilder implements GuiceInjectorBuilderConfigurer<VaultConsumerConfiguration> {

    @Override
    public GuiceInjectorBuilder apply(GuiceInjectorBuilder builder, VaultConsumerConfiguration config, Environment env) {
        DaliDBIFactory daliDBIFactory = new DaliDBIFactory(env, System.getenv("DOCKER_NAME"));
        DataSource realDataSource = daliDBIFactory.buildDataSource(config.getVaultDatabase(), "vault-db");
        DataSource avroRegistryDataSource = daliDBIFactory.buildDataSource(config.getAvroRegistryDatabase(), "avro-registry-db");
        return builder
                .addModule(new VaultDatabaseModule(realDataSource, env))
                .addModule(new AvroSchemaRegistryModule(avroRegistryDataSource, env))
                .addModule(new VaultConsumerMessagingModule())

    }
```

## The DW Application

Registers a number of resources on startup

```java
this.guiceInjector = GuiceInjectorFactory.buildGuiceInjector(configuration, env, configuration.newGuiceFactory());

        // If running with an in-memory database, set up the schema
        if (configuration.isInMemoryResources()) {
            InMemoryDatabase database = new InMemoryDatabase();
            database.start();
            InMemoryBroker broker = new InMemoryBroker();
            broker.start();
        }

        // Managed resources
        JmsSubscriber jmsSubscriber = guiceInjector.getInstance(JmsSubscriber.class);
        env.lifecycle().manage(jmsSubscriber);
        env.healthChecks().register("jms-subscriber", jmsSubscriber.getSubscriberHealthCheck());
        env.lifecycle().manage(guiceInjector.getInstance(EndOfDayConfirmDetector.class));

        // Web services
        env.jersey().register(new LoggingFilter());

        ReconciliationResource reconciliationResource = guiceInjector.getInstance(ReconciliationResource.class);
        env.jersey().register(reconciliationResource);

        env.jersey().register(guiceInjector.getInstance(EndOfDayResource.class));

        // Required by Sundial reconciliation jobs
        env.getApplicationContext().setAttribute(ReconciliationResource.class.getName(), reconciliationResource);
```
