# Jackson and Json ObjectMapping & Serialisation

## JacksonJaxbJsonProvider. Configured to use both Jackson and JAXB annotations

```java
        // create JsonProvider to provide custom ObjectMapper
        JacksonJaxbJsonProvider jacksonJaxbJsonProvider =
                new JacksonJaxbJsonProvider(mapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
```
