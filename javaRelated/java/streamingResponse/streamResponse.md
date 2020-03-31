# Streaming REST to Save on Memory stream on response

## Streaming
Saves on mem space required to return entire obj.
NB. The JsonGenerator

```java
import javax.ws.rs.core.Response;
import com.fasterxml.jackson.core.JsonGenerator

public Response getPostingSummary(LocalDate asOfDate) {

 StreamingOutput streamingOutput = output -> {
            try (JsonGenerator generator = objectMapper.getFactory().createGenerator(output)) {
                generator.writeStartArray();
                dbi.useHandle(handle ->
                        handle.createQuery(PostSummary_SQL)
                                .bind("asOfDate", asOfDate)
                                .map(new JdbiJsonResultSetMapper(objectMapper))
                                .iterator()
                                .forEachRemaining(next -> {
                                    try {
                                        generator.writeObject(next);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                }));
                generator.writeEndArray();
            } catch (IOException e) {
                throw new InternalServerErrorException(e);
            }
        };
        return Response.ok().entity(streamingOutput).build();
    }
```

## Older non-streaming version
NB. Returning jackson OBJECTnNode

```java
import com.fasterxml.jackson.databind.node.ObjectNode;

 public List<ObjectNode> getPostingSummary(LocalDate asOfDate) {
        LOGGER.info("Getting posting summary for date {}", asOfDate);
        List<ObjectNode> jsonList = dbi.withHandle(
                h -> h.createQuery(PostSummary_SQL)
                        .bind("asOfDate", asOfDate)
                        .map(new JdbiJsonResultSetMapper(objectMapper))
                        .list());
        return jsonList.stream()
                .map(o -> {
                    JsonNode failureMessage = o.get("failureMessage");
                    if (failureMessage != null && !failureMessage.isNull() && !StringUtils.isBlank(failureMessage.asText(""))) {
                        String[] allMessages = failureMessage.asText().trim().split("[\\r\\n]+");
                        String lastMessage = allMessages[allMessages.length - 1];
                        ObjectNode copy = o.deepCopy();
                        copy.set("failureMessage", new TextNode(lastMessage));
                        return copy;
                    } else {
                        return o;
                    }
                }).collect(Collectors.toList());
    }
```
