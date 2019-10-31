# Apache Commons

I really should take a good look at the library.

As well as google common for ImmutableSet, MultiMap etc..

## StringUtils
```java
StringUtils.join(emailConfig.recipients(), ",")
```

## Systemutils

```java
SystemUtils.IS_OS_WINDOWS
```

## File Utils
Opening a file of the local windows file system. *Uses the native windows app to open*
```scala
Desktop.getDesktop
        .open(file.toFile)

```

