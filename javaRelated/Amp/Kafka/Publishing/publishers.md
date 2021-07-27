# Kafka Publishers

## Event Bus (in abstractPublisher)
There is an EventBus from google guava. Allows subscribers. EventSource(s) creates events
Raises to a message bus a series of Streaming events
- Raises Exceptions/Errors: eg DeserializationExceptionEvent
- Mainy events: RecordLoadedSetStreamEvent, RecordReceivedEvent, RecordTransformingStreamEvent

### EventSource are all the many diff events sources
- AbstractComplexDomainBridgeConfigurer, AbstractKafkaStreamSingleSourceConfigurer, AbstractInputStreamSupplier 
`public interface EventSource<S extends EventSource<S>> {}

## Main abstract publisher
```java
/**
 * Generic publisher that raises events upon publishing.
 *
 * @param <P> type of concrete publisher extending this class.
 * @param <K> type of key being published.
 * @param <T> type of value being published.
 */
@Slf4j
/* @Component */
public abstract class AbstractPublisher<P extends AbstractPublisher<P, K, T>, K extends Comparable<K>, T> implements Consumer<KStream<K, T>>, EventSource<P> {

    /**
     * Eventf
     */
    public class StreamPublishingEvent extends StreamingEvent<P, KeyValue<K, T>> {
        protected StreamPublishingEvent(KeyValue<K, T> payload) {
            super((P) AbstractPublisher.this, payload);
        }
    }

    @NonNull
    private class LoggingSerde<U> implements Serde<U> {
        private final String          kind;
        private final Serde<U>        source;
        private final Serializer<U>   serializer;
        private final Deserializer<U> deserializer;

        private LoggingSerde(@NonNull String kind, @NonNull Serde<U> source) {
            this.kind = kind;
            this.source = source;
            this.serializer = new Serializer<U>() {
                final Serializer<U> serializer = source.serializer();

                @Override
                public byte[] serialize(String topic, U data) {
                    final var result = serializer.serialize(topic, data);
                    log.info("Serialized {}='{}' into {} bytes for topic '{}'", kind, data, result.length, topic);
                    return result;
                }
            };
            this.deserializer = source.deserializer();
        }

        @Override
        public Serializer<U> serializer() {
            return serializer;
        }

        @Override
        public Deserializer<U> deserializer() {
            return deserializer;
        }
    }

    @Autowired(required = false)
    @Getter
    @Setter
    private SharedEventBus eventBus;

    /**
     * Serde for keys.
     */
    @Getter
    protected final Serde<K> keySerde;
    /**
     * Serde for values.
     */
    @Getter
    protected final Serde<T> valueSerde;

    /**
     * Builds an instance with a preset event bus.
     *
     * @param keySerde   serde for keys to publish.
     * @param valueSerde serde for values to publish.
     */
    protected AbstractPublisher(@NonNull Serde<K> keySerde, @NonNull Serde<T> valueSerde) {
        this.keySerde   = keySerde;
        this.valueSerde = valueSerde;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ //

    @PostConstruct
    private void init() {
        if (eventBus == null) {
            eventBus = SharedEventBus.instance().orElseThrow(() -> new UnsupportedOperationException("No event bus"));
        }
    }

    @Override
    public void accept(@NonNull KStream<K, T> stream) {
        final var topic      = topicName();
        final var keySerde   = new LoggingSerde<>("key", this.keySerde);
        final var valueSerde = new LoggingSerde<>("value", this.valueSerde);
        stream.peek((k, v) -> log.info("Publishing key='{}', value='{}' --> topic='{}' ...",
                                       Objects.toString(k),
                                       v,
                                       topic))
              .peek((k, v) -> eventBus.raiseEvent(new StreamPublishingEvent(KeyValue.pair(k, v))))
              .to(topic, Produced.with(keySerde, valueSerde));
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ //

    /**
     * Name of the topic to publish to.
     *
     * @return topic name.
     */
    protected abstract String topicName();
}
```

## JsonStringPublisher

```java
/**
 * Specialised published that serializes to JSON and it optionally unquotes the resulted strings.
 *
 * @param <P> type of concrete publisher extending this class.
 * @param <K> type of key being published.
 * @param <T> type of value being published.
 */
/* @Component */
public class JsonStringPublisher<P extends JsonStringPublisher<P, K, T>, K extends Comparable<K>, T> extends AbstractPublisher<JsonStringPublisher<P, K, T>, K, T> {

    @Getter
    private final String topicName;
    @Getter
    private final Class<K> keyClass;
    @Getter
    public final Class<T> valueClass;

    /**
     * Builds an instance based on default serdes.
     *
     * @param topicName     name of the topic to publish to.
     * @param keyClass      type of key.
     * @param valueClass    type of value.
     * @param unquoteResult true to unquote the results if strings.
     */
    public JsonStringPublisher(@NonNull String topicName,
                               @NonNull Class<K> keyClass,
                               @NonNull Class<T> valueClass,
                               boolean unquoteResult) {
        this(topicName, keyClass, valueClass, getRawType(keyClass), getRawType(valueClass), unquoteResult);
    }

    /**
     * Builds an instance based on given Java types.
     *
     * @param topicName     name of the topic to publish to.
     * @param keyClass      type of key.
     * @param valueClass    type of value.
     * @param keyJavaType   Java type for key.
     * @param valueJavaType Java type for value.
     * @param unquoteResult true to unquote the results if strings.
     */
    public JsonStringPublisher(@NonNull String topicName,
                               @NonNull Class<K> keyClass,
                               @NonNull Class<T> valueClass,
                               @NonNull JavaType keyJavaType,
                               @NonNull JavaType valueJavaType,
                               boolean unquoteResult) {
        this(topicName, keyClass, valueClass, getRawSerde(keyJavaType, true), getRawSerde(valueJavaType, false), unquoteResult);
    }


    /**
     * Builds an instance based on given serdes.
     *
     * @param topicName     name of the topic to publish to.
     * @param keyClass      type of key.
     * @param valueClass    type of value.
     * @param keySerde      serde for key.
     * @param valueSerde    serde for value.
     * @param unquoteResult true to unquote the results if strings.
     */
    public JsonStringPublisher(@NonNull String topicName,
                               @NonNull Class<K> keyClass,
                               @NonNull Class<T> valueClass,
                               @NonNull Serde<K> keySerde,
                               @NonNull Serde<T> valueSerde,
                               boolean unquoteResult) {
        super(unquotedSerde(keyClass, keySerde, unquoteResult), unquotedSerde(valueClass, valueSerde, unquoteResult));
        this.topicName = topicName;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
    }

    private static <U> Serde<U> unquotedSerde(Class<U> clazz, Serde<U> source, boolean unquoteResult) {
        final Serde<U> result;
        if (unquoteResult && String.class.isAssignableFrom(clazz)) {
            result = (Serde<U>) new Serdes.StringSerde();
        } else {
            result = source;
        }
        return result;
    }

    @Override
    protected String topicName() {
        return topicName;
    }
}
```
