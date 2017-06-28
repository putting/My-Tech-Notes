# Java Reduce

## Using Reduce with a Stream to add multiple fields in obe pass
- You can use this approach with a stream of fields to add
``` java
public BigDecimal addFields(List<Thing> things) {
    return things.parallelStream()
        .flatMap(thing -> Stream.of(thing.getField1(), thing.getField2()))
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

- Or you neeed to create a new Object and reduce it
``` java
MyObject totals = myListOfObjects.stream()
    .reduce((x, y) -> new MyObject(
        x.getA() + y.getA(),
        x.getB() + y.getB(),
        x.getC() + y.getC()
    ))
    .orElse(new MyObject(0, 0, 0));

totalA = totals.getA();
totalB = totals.getB();
totalC = totals.getC();
```
