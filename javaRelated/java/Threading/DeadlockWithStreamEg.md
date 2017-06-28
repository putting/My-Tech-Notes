# Deadlock With Stream Example


```java 
Object[] locks = { new Object(), new Object() };
 
IntStream
    .range(1, 5)
    .parallel()
    .peek(Unchecked.intConsumer(i -> {
        synchronized (locks[i % locks.length]) {
            Thread.sleep(100);
 
            synchronized (locks[(i + 1) % locks.length]) {
                Thread.sleep(50);
            }
        }
    }))
    .forEach(System.out::println);
```
