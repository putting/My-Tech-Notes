# ORM Stuff

## Jdbi

## SimpleFlatMapper
Looks like a good/simpl approach to ORM. Can be used with jdbi3.
Supports Immutables.
(http://simpleflatmapper.org/)

## Mercuria Impl
We currently use Interfaces annotated with 
  - Immutables @Value
  - Jackson json @Json
  - @Jdbi is in fact a dali (mercuria) annotations
  
NB. Mappings are generated using custom code and the nnotations to create the mappers at compile times.  
  
```java
@Value.Immutable
@JsonSerialize(as = ImmutableWorkflow.class)
@JsonDeserialize(as = ImmutableWorkflow.class)
@Jdbi(tableName = "control.control_break_workflow")
interface ABC {
    @Value.Default
    @JdbiOptions(inColumnList = false)
        default long id() {
        return -1;
    }
        @Value.Auxiliary
    Instant validFrom();
    
}

```
