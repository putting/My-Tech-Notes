# Guice & Reflections

## Can bind from multiple modules and use reflection to get at all classes implementing an interface (using annotations)

Here multiple sub types of interface Check are being bound via Guice. Similar in the way all the Dao's can be bound.
Its also using a reflection lib **SubTypesScanner** to get all the implementations of the Check interface.
Based on annotations ICTSAudit.class, a different provider for each db date. Live queries the pl_history table, 
whereas audit always uses the last day of the month.

```java
        Multibinder<Check> checkBinder = Multibinder.newSetBinder(binder(), Check.class);
        new Reflections("com.mercuria.giant.icts", new SubTypesScanner())
                .getSubTypesOf(Check.class)
                .forEach(implementation -> checkBinder.addBinding().to(implementation));

        if(ictsInstanceAnnotation.equals(ICTSAudit.class)){
            bind(BusinessDateProvider.class).to(AuditBusinessDateProvider.class).in(Singleton.class);
        } else{
            bind(BusinessDateProvider.class).to(LiveBusinessDateProvider.class).in(Singleton.class);
        }

        bind(ICTSControlRunner.class).in(Singleton.class);
        bind(ICTSControlRunner.class).annotatedWith(ictsInstanceAnnotation).to(ICTSControlRunner.class);
        expose(ICTSControlRunner.class).annotatedWith(ictsInstanceAnnotation);
```
