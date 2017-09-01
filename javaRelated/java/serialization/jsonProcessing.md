# Serialization Tips & Good Practice

## Using Jackson libs as part of Dropwizard

### Example of parsing a result set as a json record. Later the individual items can be iterated below 

```java
public class JsonRecordIterator<T> implements Iterator<T>, Closeable {

    private final JsonParser parser;
    private final ObjectMapper objectMapper;
    private final Iterator<T> nestedIterator;
    private boolean closed = false;

    public JsonRecordIterator(ObjectMapper objectMapper, Reader reader, Class<T> recordType) throws IOException {
        this.objectMapper = objectMapper;
        this.parser = objectMapper.getFactory().createParser(reader);
        JsonToken firstToken = parser.nextToken();
        if (firstToken!= JsonToken.START_ARRAY) {
            throw new IllegalArgumentException("JSON structure is not an array: " + firstToken);
        }
        if (parser.nextToken() == JsonToken.END_ARRAY) {
            this.nestedIterator = Collections.emptyIterator();
        } else {
            this.nestedIterator = objectMapper.readValues(parser, recordType);
        }

    }

    @Override
    public boolean hasNext() {
        boolean hasNext = nestedIterator.hasNext();
        if (hasNext == false) {
            try {
                close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return hasNext;
    }

    @Override
    public T next() {
        return nestedIterator.next();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return; // already closed
        }
        try {
            JsonToken lastToken = parser.currentToken();
            if (lastToken != JsonToken.END_ARRAY) {
                throw new IllegalArgumentException("JSON structure does not end with an end of array: " + lastToken);
            }
            JsonToken eof = parser.nextToken();
            if (eof != null) {
                throw new IllegalArgumentException("JSON structure contains data after array:" + eof);

            }
        } finally {
            closed = true;
            parser.close();
        }
    }

    public Stream<T> toStream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
                false);
    }

}

```
And iterating over the record


```java
    JsonGenerator generator = objectMapper.getFactory().createGenerator(stringWriter);
    generator.writeStartArray();

    JsonRecordIterator<ObjectNode> iterator = new JsonRecordIterator<>(objectMapper, stringReader, ObjectNode.class);
    while (iterator.hasNext()) {

        ObjectNode nextNode = iterator.next();
        //Add the portfolioEodId as a node
        nextNode.put("portfolioEodId", portfolioEodId());

        generator.writeObject(nextNode);
    }

    generator.writeEndArray();
    generator.close();
```
