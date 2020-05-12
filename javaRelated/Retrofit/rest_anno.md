# REst annotation to mark classes for generation

```java
package com.mercuria.dali.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add to an interface to generate a JAX-RS service + impl that delegates to this
 * Will also generate a web service client using retrofit that implements this interface
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Rest {
}

package com.mercuria.dali.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this to a JAX-RS annotated class / interface to generate a Retrofit client
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RestClient {
    /**
     * This should only be set via the compiler
     */
    String interfaceClass() default "";

    Class<?>[] paramTypesToIgnore() default {};
}

package com.mercuria.dali.rest;

import com.fasterxml.jackson.databind.ObjectMapper;

package com.mercuria.dali.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to indicate to client gen code that the result is streamed into this originalType
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Streamed {
    String originalType();
}


@FunctionalInterface
public interface ClientBuilder<T, C> {

    C apply(T target, ObjectMapper objectMapper, int maxRetries);
}


```
