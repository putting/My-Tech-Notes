# Jackson ObjectMapper

Ensure initiatialised as:

ObjectMapper mapper = new ObjectMapper();
mapper.enable(SerializationFeature.INDENT_OUTPUT);
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
mapper.registerModule(new GuavaModule());
mapper.registerModule(new JavaTimeModule());
mapper.registerModule(new Jdk8Module());
