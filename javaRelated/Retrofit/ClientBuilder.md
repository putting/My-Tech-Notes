package com.paul;

import com.fasterxml.jackson.databind.ObjectMapper;

@FunctionalInterface
public interface ClientBuilder<T, C> {

    C apply(T target, ObjectMapper objectMapper, int maxRetries);
}
