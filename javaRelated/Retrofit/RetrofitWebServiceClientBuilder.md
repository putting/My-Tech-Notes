# Used as a builder for Retrofit generated classes.

Appears to set some http params and config. Uses OkHttpClient

```java
package com.paul;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.concurrent.TimeUnit;

public final class RetrofitWebServiceClientBuilder<T, C> {

    private final String baseUrl;
    private final Class<T> serviceType;
    private final Logger logger;
    private final ClientBuilder<T, C> builder;

    private Interceptor authInterceptor = null;
    private ObjectMapper objectMapper = new ObjectMapper();
    private long timeout = TimeUnit.SECONDS.toMillis(30);
    private int maxRetries = 10;

    public RetrofitWebServiceClientBuilder(String baseUrl, Logger logger, Class<T> serviceType, ClientBuilder<T, C> builder) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : (baseUrl + "/");
        this.logger = logger;
        this.serviceType = serviceType;
        this.builder = builder;
    }

    public RetrofitWebServiceClientBuilder<T, C> withBasicAuth(String userName, String password) {
        this.authInterceptor = chain -> {
            String credential = Credentials.basic(userName, password);
            return chain.proceed(chain.request()
                    .newBuilder()
                    .header("Authorization", credential)
                    .build());
        };
        return this;
    }

    public RetrofitWebServiceClientBuilder<T, C> withObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public RetrofitWebServiceClientBuilder<T, C> withTimeout(long time, TimeUnit timeUnit) {
        this.timeout = timeUnit.toMillis(time);
        return this;
    }

    public RetrofitWebServiceClientBuilder<T, C> withMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public RetrofitWebServiceClientBuilder<T, C> withNoRetry() {
        this.maxRetries = 0;
        return this;
    }

    public C build() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(message -> {
            if (logger.isTraceEnabled()) {
                logger.trace(message);
            } else if (logger.isDebugEnabled()) {
                logger.debug(message);
            } else if (logger.isInfoEnabled()) {
                logger.info(message);
            }
        });
        if (logger.isTraceEnabled()) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else if (logger.isDebugEnabled()) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        } else if (logger.isInfoEnabled()) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .readTimeout(this.timeout, TimeUnit.MILLISECONDS);
        if (this.authInterceptor != null) {
            clientBuilder = clientBuilder.addInterceptor(this.authInterceptor);
        }
        T api = new Retrofit.Builder()
                .client(clientBuilder.build())
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(this.objectMapper))
                .build()
                .create(serviceType);
        return this.builder.apply(api, this.objectMapper, this.maxRetries);
    }

}
```
