# Spark & Jetty ACL

## Cross-Origin. 
No sure what is, but I think it allows credentials for one REST resource to be forwarded onto another REST service.

```java
FilterHolder cors =
                context.addFilter(CrossOriginFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
```
