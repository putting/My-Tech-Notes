# Awaitility Library

Good library for waiting a certain time during tests.

eg.Waits at most 1 sec, but can complete earlier based on thje listener receiving the msg and setting found to true
```java

    @Autowired
    KafkaListenerBean             listener;
    @Autowired
    KafkaTemplate<Long, Str>      template;

@Test
    void receiveOneMessage() {
        final long key   = random.nextLong();
        final val  value = new Str(Double.toString(random.nextDouble()));
        final val  found = new AtomicBoolean(false);
        final val  since = new AtomicLong(0L);
        final val  until = new AtomicLong(0L);
        listener.setTestMethod((k, v) -> {
            until.set(System.currentTimeMillis());
            if (key == k.longValue() && value.equals(v)) {
                if (!found.get()) {
                    until.set(System.currentTimeMillis());
                }
                found.set(true);
            }
        });
        since.set(System.currentTimeMillis());
        template.send(SOURCE_TOPIC, key, value)
                .addCallback(result -> {
                                 since.set(System.currentTimeMillis());
                                 System.out.printf("Sent key=%d, value=%s%n",
                                                   result.getProducerRecord().key(),
                                                   result.getProducerRecord().value());
                             },
                             err -> log.error("Test error", err));
        await().atMost(1, TimeUnit.SECONDS).until(found::get);
        System.out.printf("Sending/receiving 1 message took %d ms%n", until.longValue() - since.longValue());
    }
```

not sure how the listener listens to kafk. MessageListener is kafka lib and the test method sets the found.set flag

```java
@Component
public class KafkaListenerBean implements MessageListener<Long, Str> {

    private BiConsumer<Long, Str> testMethod;

    /**
     * Injects a test callback into this class.
     */
    public void setTestMethod(@NonNull BiConsumer<Long, Str> testMethod) {
        this.testMethod = testMethod;
    }

    @Override
    public void onMessage(ConsumerRecord<Long, Str> data) {
        testMethod.accept(data.key(), data.value());
    }
}
```
