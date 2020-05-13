# RestHelper

package com.mercuria.dali.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

public class RestHelper {

    private RestHelper() {
    }

    public static <T> Response stream(ObjectMapper objectMapper, Stream<T> input) {
        StreamingOutput streamingOutput = output -> {
            try (JsonGenerator generator = objectMapper.getFactory().createGenerator(output)) {
                generator.writeStartArray();
                input.forEach(next -> {
                    try {
                        generator.writeObject(next);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                generator.writeEndArray();
            } catch (IOException e) {
                throw new DaliWebServiceException(e);
            }
        };
        return Response.ok().entity(streamingOutput).build();
    }
}
