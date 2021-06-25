# Exceptions

## MultiFailureException
- Used in try. ```try(t = new MultiFailureThrower) ```
- The key part is that any Ex thrown will CLOSE the resource as MultiFailureThrower implements Autocloseable. Allowing erros to be accumulated
- Also raises failures on an Event Bus so registered listeners can react to errors. @See SharedEventBus. **Uses EventBus from Guava**
