# This should come under Async dir


## AsyncExecutorService
Helper class which wraps Async utils.
eg. waitForAll is essentially `CompletableFuture.allOf()` but add timeout and ex handling and support for values at timeout.
eg `call` add MultiFailure ex handling, but return a completedFuture. Not sure how it can 

### NB. The Async annotation means there is an Executor backing the calls in this class and will run in sep thread

```java
@Service
@Async(ASYNC_EXECUTOR)
@Slf4j
public class AsyncExecutorService {

    /**
     * Waits for a stream of completable futures and returnes their results in the order they are given.
     * <p></p>
     * If any future times out, then it gets cancelled and its result is taken as the value on timeout.
     * <p></p>
     * The total waiting time may be slightly longer than the given timeout by one millisecond for each future given.
     *
     * @param <T>            type of results.
     * @param futureResults  stream of {@link CompletableFuture} yielding the future results.
     * @param timeoutMillis  number of milliseconds to wait for the results.
     * @param valueOnTimeout value to supply if a completable future times out.
     *
     * @return list of results in the order of the futures.
     */
    public static <T> List<T> waitForAll(@NonNull Stream<CompletableFuture<T>> futureResults, long timeoutMillis, T valueOnTimeout) {
        final List<CompletableFuture<T>> futureList = futureResults.collect(Collectors.toList());
        final CompletableFuture<T>       futureAll  = (CompletableFuture<T>) CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
        try {
            //
            // Issue a get to wait with timeout
            //
            futureAll.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch(InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        } catch(TimeoutException e) {
            //
            // Timeout; handle by providing the default timeout value
            //
        }
        //
        // Collect the results and return
        //
        final List<T> result = futureList.stream()
                                         .map(f -> Pair.of(f, f.getNow(valueOnTimeout)))
                                         .peek(pair -> {
                                             if (!pair.getLeft().isDone()) {
                                                 pair.getLeft().cancel(false);
                                             }
                                         })
                                         .map(Pair::getRight)
                                         .collect(Collectors.toList());
        return result;
    }

    /**
     * Waits for an acceptable value to be supplied.
     *
     * @param <T>            type of value to wait for.
     * @param valueName      name of value to wait for.
     * @param supplier       value supplier.
     * @param acceptor       predicate indicating the value is acceptable.
     * @param timeoutMillis  number of milliseconds to wait for the value.
     * @param valueOnTimeout value to return on timeout.
     *
     * @return instance of {@link CompletableFuture} returning the value to wait for or the value on timeout if the timeout expired.
     */
    public <T> CompletableFuture<T> waitForAccepted(String valueName,
                                                    @NonNull Supplier<T> supplier,
                                                    @NonNull Predicate<T> acceptor,
                                                    long timeoutMillis,
                                                    T valueOnTimeout) {
        try(var t = new MultiFailureThrower()) {
            assert t != null;
            return waitForAcceptedOrLast(valueName,
                                         supplier,
                                         acceptor,
                                         timeoutMillis,
                                         (lastValue, expired) -> expired ? valueOnTimeout : lastValue);
        }
    }

    /**
     * Waits for an acceptable value to be supplied.
     *
     * @param <T>           type of value to wait for.
     * @param <U>           type of result.
     * @param valueName     name of value to wait for.
     * @param supplier      value supplier.
     * @param acceptor      predicate indicating the value is acceptable.
     * @param timeoutMillis number of milliseconds to wait for the value.
     * @param onLastValue   transformation to apply upon the last supplied value to obtain the result. The first argument is the last value supplied, the second
     *                      argument is whether the timeout expired.
     *
     * @return instance of {@link CompletableFuture} returning the value to wait for or the transformed last value if the timeout expired.
     */
    public <T, U> CompletableFuture<U> waitForAcceptedOrLast(String valueName,
                                                             @NonNull Supplier<T> supplier,
                                                             @NonNull Predicate<T> acceptor,
                                                             long timeoutMillis,
                                                             @NonNull BiFunction<T, Boolean, U> onLastValue) {
        try(var t = new MultiFailureThrower()) {
            assert t != null;
            final long since = System.currentTimeMillis();
            final Callable<U> timedSupplier = () -> {
                T       lastValue;
                boolean expired = true;
                do {
                    log.info("Waiting for value '{}' ...", valueName);
                    lastValue = supplier.get();
                    if (acceptor.test(lastValue)) {
                        expired = System.currentTimeMillis() - since > timeoutMillis;
                        break;
                    } else {
                        log.info("Value '{}' is not acceptable: {}", valueName, lastValue);
                    }
                } while (System.currentTimeMillis() - since < timeoutMillis);
                final U result = onLastValue.apply(lastValue, expired);
                if (expired) {
                    log.warn("No acceptable value '{}' has been supplied; using last result: {}", valueName, result);
                } else {
                    log.info("Returning acceptable result for '{}': {}", valueName, result);
                }
                return result;
            };
            return call(timedSupplier, "waiting %d ms for %s", timeoutMillis, valueName);
        }
    }

    /**
     * Converts a {@link Runnable} into a {@link Callable} returning true.
     *
     * @param runnable instance of {@link Runnable} to convert.
     *
     * @return converted {@link Callable}.
     */
    private static Callable<Boolean> toCallable(Runnable runnable) {
        return () -> {
            runnable.run();
            return true;
        };
    }

    /**
     * Calls the given callable asynchronously and returns the result.
     *
     * @param <T>      type of result to return asynchronously.
     * @param callable instance of {@link Callable} to call asynchronously.
     * @param format   string format describing the call.
     * @param args     format arguments describing the call.
     *
     * @return instance of {@link CompletableFuture} containing the result.
     */
    public <T> CompletableFuture<T> call(Callable<T> callable, String format, Object... args) {
        try(var t = new MultiFailureThrower()) {
            assert t != null;
            final String callName = String.format(format, args);
            final long   since    = System.currentTimeMillis();
            log.info("Calling '{}' ...", callName);
            try {
                final T result = callable.call();
                log.info("Returning {} from '{}' took {} ms", result, callName, System.currentTimeMillis() - since);
                return CompletableFuture.completedFuture(result);
            } catch(Exception ex) {
                log.error(String.format("Error calling %s after %d ms", callName, System.currentTimeMillis() - since), ex);
                return CompletableFuture.failedFuture(ex);
            }
        }
    }

    /**
     * Runs the given runnable asynchronously and returns true on completion.
     *
     * @param runnable instance of {@link Runnable} to run asynchronously.
     * @param format   string format describing the run.
     * @param args     format arguments describing the call.
     *
     * @return instance of {@link CompletableFuture} containing true.
     */
    public CompletableFuture<Boolean> run(Runnable runnable, String format, Object... args) {
        try(var t = new MultiFailureThrower()) {
            assert t != null;
            return call(toCallable(runnable), format, args);
        }
    }
}
```
