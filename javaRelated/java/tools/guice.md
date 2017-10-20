# Guice

Lots to learn on this.

## Provider
see [Injecting Providers](https://github.com/google/guice/wiki/InjectingProviders)
Used in giant to inject multiple implementations of Check interface into a service.
**By magic it finds all the impls and injects....**
```java
@Inject
    public ICTSControlRunner(Set<Check> checks, BusinessDateProvider businessDateProvider) {
        this.checks = checks;
        this.businessDateProvider = businessDateProvider;
    }
```    
