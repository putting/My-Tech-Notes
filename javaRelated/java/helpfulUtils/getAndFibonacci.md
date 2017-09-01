# Useful wrappers for getting values & Fibonacci back off

```java
public class RetrofitHelper {

    //'exponential backoff using fibonacci seq'
    private static final int[] FIBONACCI_SEQ = new int[]{0, 1, 1, 2, 3, 5, 8, 13};
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int GATEWAY_TIMEOUT = 504;

    private RetrofitHelper() {
    }

    public static <T> T getValue(Logger logger, Supplier<Call<T>> callSupplier, boolean isOptional) {
        Response<T> response = execute(logger, callSupplier, 0);
        if (response.isSuccessful()) {
            return response.body();
        } else if (isOptional) {
            return (T) Optional.empty();
        } else {
            throw new DaliWebServiceClientException(response.message());
        }
    }

    public static <T> Stream<T> streamValues(Logger logger, Supplier<Call<ResponseBody>> callSupplier, ObjectMapper objectMapper, Class<T> clazz) {
        Response<ResponseBody> response = RetrofitHelper.execute(logger, callSupplier, 0);
        if (response.isSuccessful()) {
            try {
                return Streams.stream(objectMapper.readerFor(clazz).readValues(response.body().byteStream()));
            } catch (IOException e) {
                throw new DaliWebServiceClientException(e);
            }
        } else {
            throw new DaliWebServiceClientException(response.message());
        }
    }

    private static <T> Response<T> execute(Logger logger, Supplier<Call<T>> callSupplier, int attempt) {
        Call<T> tCall = callSupplier.get();
        try {
            Response<T> response = tCall.execute();
            if (attempt > 0) {
                logger.debug("Attempt {}, will sleep and retry.  Url {}", attempt, tCall.request().url());
                try {
                    Thread.sleep(FIBONACCI_SEQ[attempt] * 1000);
                } catch (InterruptedException e) {
                    throw new DaliWebServiceClientException(e);
                }
            }
            if (canRetry(attempt, response)) {
                return execute(logger, callSupplier, attempt + 1);
            } else {
                return response;
            }
        } catch (IOException ioe) {
            logger.warn("IOException calling rest service.   Url {}", ioe, tCall.request().url());
            if (attempt < FIBONACCI_SEQ.length - 1) {
                return execute(logger, callSupplier, attempt + 1);
            } else {
                throw new DaliWebServiceClientException(ioe);
            }
        }
    }

    private static <T> boolean canRetry(int attempt, Response<T> response) {
        boolean tempErrorStatusCode = response.code() == SERVICE_UNAVAILABLE || response.code() == GATEWAY_TIMEOUT;
        return tempErrorStatusCode && attempt != FIBONACCI_SEQ.length - 1;
    }
}
```
