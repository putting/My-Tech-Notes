# AsynFlatMap

Complicated eg of flattening the incoming source stream. I presume Multiple values per key. Or the values are arrays/lists.
TODO: Investigate further

```java
/**
         * Gets the Kafka stream resulted by flattening asynchronously the source stream.
         *
         * @param <K>             type of keys in the source stream.
         * @param <V>             type of values in the source stream.
         * @param <KR>            type of keys in the resulted stream.
         * @param <VR>            type of values in the resulted stream.
         * @param stream          type of source stream to map.
         * @param storeName       name of the store used in stream processing.
         * @param persistentStore true to use a store persisted on disk.
         * @param keyJavaType     {@link JavaType} of key.
         * @param valueJavaType   {@link JavaType} of value.
         * @param mapper          asynchronous mapping function.
         * @param timeout         duration to wait for each mapping to complete.
         * @param concurrency     how many mappings to carry out concurrently.
         *
         * @return resulted {@link KStream}.
         *
         * @see KStream#flatMap(KeyValueMapper)
         * @see #asyncFlatMap(KStream, Storing.StoreName, boolean, JavaType, JavaType, KeyValueMapper, Duration)
         */
        public static <K extends Comparable<K>, V, KR, VR> KStream<KR, VR> asyncFlatMap(KStream<K, V> stream,
                                                                                        Storing.StoreName storeName,
                                                                                        boolean persistentStore,
                                                                                        JavaType keyJavaType,
                                                                                        JavaType valueJavaType,
                                                                                        KeyValueMapper<? super K,
                                                                                                ? super V,
                                                                                                ? extends CompletableFuture<? extends Iterable<? extends KeyValue<? extends KR, ? extends VR>>>> mapper,
                                                                                        Duration timeout,
                                                                                        int concurrency) {
            final Function<KeyValue<K, V>[], CompletableFuture<KeyValue<KR, VR>[]>> superMapperFun = kvArr ->
            {
                final ConcurrentHashMap<KeyValue<K, V>, List<KeyValue<KR, VR>>> collected = new ConcurrentHashMap<>(kvArr.length * 4 / 3);
                final CompletableFuture<Void> combined = CompletableFuture.allOf(Stream.of(kvArr)
                                                                                       .peek(kv -> log.info("Loading asynchronously for k={}, v={} ...", kv.key, kv.value))
                                                                                       .map(kv -> Pair.of(kv,
                                                                                                          mapper.apply(kv.key, kv.value)
                                                                                                                .thenApply(it ->
                                                                                                                           {
                                                                                                                               final var lst = (Collection<KeyValue<KR, VR>>) (Collection<?>) toCollection(it);
                                                                                                                               log.info("Loaded {} elements for k={}, v={}", lst, kv.key, kv.value);
                                                                                                                               return lst;
                                                                                                                           })))
                                                                                       .map(pair -> pair.getRight()
                                                                                                        .thenAccept(lst -> collected.computeIfAbsent(pair.getLeft(),
                                                                                                                                                     qw -> Collections.synchronizedList(new LinkedList<>()))
                                                                                                                                    .addAll(lst)))
                                                                                       .toArray(n -> new CompletableFuture[n]));
                final CompletableFuture<KeyValue<KR, VR>[]> result = combined.thenApply(v -> collected)
                                                                             .thenApply(map -> Stream.of(kvArr)
                                                                                                     .map(map::get)
                                                                                                     .flatMap(Collection::stream)
                                                                                                     .toArray(n -> new KeyValue[n]));
                return result;
            };
            final KStream<KR, VR> result = asyncMapper(stream, storeName, persistentStore, keyJavaType, valueJavaType, superMapperFun, timeout, concurrency);
            return result;
        }
```
