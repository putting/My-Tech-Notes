# Guava caching Utils

Useful for holding caches of ref data etc..

## The client code

```java
@Singleton
public class ICTSCostService {

    private final CacheUtils.SingleObjectCache<List<String>> costTypeCodeCache;

    @Inject
    public ICTSCostService(@ICTSDatabase ICTSCostDao dao) {
        this.costTypeCodeCache = CacheUtils.singleObjectCache(dao::getCostTypeCodes);
    }

    public List<String> getCostTypeCodes() {
        return this.costTypeCodeCache.getUnchecked();
    }

}
```

## This is where the Guava cache objs are used

```java
public class CacheUtils {
    private static final String ARBITRARY_KEY = "arbitrary key";

    //Wrapper Class to ensure that only One key is stored in the LoadingCache
    //Alternatively, see:
    //https://google.github.io/guava/releases/19.0/api/docs/com/google/common/base/Suppliers.html#memoizeWithExpiration(com.google.common.base.Supplier,%20long,%20java.util.concurrent.TimeUnit)
    public static class SingleObjectCache<T> {
        private final LoadingCache<String, T> cache;

        private SingleObjectCache(LoadingCache<String, T> cache) {
            this.cache = cache;
        }

        public T getUnchecked() {
            return cache.getUnchecked(ARBITRARY_KEY);
        }

        public T get() throws ExecutionException {
            return cache.get(ARBITRARY_KEY);
        }
    }

    public static <T> SingleObjectCache<T> singleObjectCache(Supplier<T> loader) {
        LoadingCache<String, T> cache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<Object, T>() {
                            public T load(Object key) {
                                return loader.get();
                            }
                        }
                );

        return new SingleObjectCache<>(cache);
    }

    public static <K,V> LoadingCache<K,V> buildLimitedCache(Function<K,V> loader) {
        return CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<K, V>() {
                            public V load(K key) {
                                return loader.apply(key);
                            }
                        }
                );
    }

}
```
