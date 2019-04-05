# Helper around http retries and erros

Note the supplier which in generated code does this:
Supplier<Call<ResponseBody>> responseSupplier = () -> this.clientApi.clearedDerivatives(businessDate);

```java
package com.paul;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class RetrofitHelper {

    private static final ObjectMapper jsonParser = new ObjectMapper();

    //'exponential backoff using fibonacci seq'
    private static final int[] FIBONACCI_SEQ = new int[]{0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144};
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int GATEWAY_TIMEOUT = 504;

    private RetrofitHelper() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValue(Logger logger, Supplier<Call<T>> callSupplier, boolean isOptional, int maxRetries) {
        Response<T> response = execute(logger, callSupplier, 0, maxRetries);
        if (response.isSuccessful()) {
            return response.body();
        } else if (isOptional) {
            return (T) Optional.empty();
        } else {
            throw newResponseException(response);
        }
    }

    public static <T> Stream<T> streamValues(Logger logger, Supplier<Call<ResponseBody>> callSupplier, ObjectMapper objectMapper, Class<T> clazz, int maxRetries) {
        Response<ResponseBody> response = RetrofitHelper.execute(logger, callSupplier, 0, maxRetries);
        if (response.isSuccessful()) {
            try {
                return Streams.stream(objectMapper.readerFor(clazz).readValues(response.body().byteStream()));
            } catch (IOException e) {
                throw new DaliWebServiceClientException(e);
            }
        } else {
            throw newResponseException(response);
        }
    }

    private static <T> Response<T> execute(Logger logger, Supplier<Call<T>> callSupplier, int attempt, int maxRetries) {
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
            if (canRetry(attempt, maxRetries, response)) {
                return execute(logger, callSupplier, attempt + 1, maxRetries);
            } else {
                return response;
            }
        } catch (IOException ioe) {
            logger.warn("IOException calling rest service.   Url {}", ioe, tCall.request().url());
            if (attempt < FIBONACCI_SEQ.length - 1) {
                return execute(logger, callSupplier, attempt + 1, maxRetries);
            } else {
                throw new DaliWebServiceClientException(ioe);
            }
        }
    }

    private static <T> boolean canRetry(int attempt, int maxRetries, Response<T> response) {
        boolean tempErrorStatusCode = response.code() == SERVICE_UNAVAILABLE || response.code() == GATEWAY_TIMEOUT;
        int userDefinedMaxRetries = Math.max(0, maxRetries);
        int maxAvailableRetries = Math.min(userDefinedMaxRetries, FIBONACCI_SEQ.length - 1);
        return tempErrorStatusCode && attempt < maxAvailableRetries;
    }


    private static <T> DaliWebServiceClientException newResponseException(Response<T> response) {
        return new DaliWebServiceClientException(response.raw().code(), extractErrorMessage(response));
    }

    private static <T> String extractErrorMessage(Response<T> response) {

        // Attempt to read any Dropwizard-format error body
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null && errorBody.contentType() != null && errorBody.contentType().toString().equals("application/json")) {
            try {
                ObjectNode error = jsonParser.readValue(errorBody.bytes(), ObjectNode.class);
                JsonNode messageNode = error.get("message");
                return messageNode.asText();
            } catch (IOException ioe) {
                // Fall through
            }
        }

        return response.message();
    }

}
```
