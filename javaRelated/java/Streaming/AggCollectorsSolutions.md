# Grouping and Aggregations With Java Streams
https://dzone.com/articles/grouping-and-aggregations-with-java-streams

Has some v good solutions (incl records) to:
- Agg multi fields
- Agg to composite keys



## Multi field by Composite key using Map
```java
public class TaxEntry {
    private String state;
    private String city;
    private BigDecimal rate;
    private BigDecimal price;
    record StateCityGroup(String state, String city) {
    }
    //Constructors, getters, hashCode/equals etc
}
```
Good, However, it has the drawback that it does not have any finalizers that allow you to perform extra operations. 
eg. you can’t do averages of any kind.
```java
Map<StateCityGroup, RatePriceAggregation> mapAggregation = taxes.stream().collect(
      toMap(p -> new StateCityGroup(p.getState(), p.getCity()), 
            p -> new RatePriceAggregation(1, p.getRate().multiply(p.getPrice())), 
            (u1,u2) -> new RatePriceAggregation( u1.count() + u2.count(), u1.ratePrice().add(u2.ratePrice()))
            ));
```

## Final and most complex sollution, but can be generalized to many problems:
```java
Map<StateCityGroup, TaxEntryAggregation> groupByAggregation = taxes.stream().collect(
    groupingBy(p -> new StateCityGroup(p.getState(), p.getCity()), 
               mapping(p -> new TaxEntryAggregation(1, p.getRate().multiply(p.getPrice()), p.getPrice()), 
                       collectingAndThen(reducing(new TaxEntryAggregation(0, BigDecimal.ZERO, BigDecimal.ZERO),
                                                  (u1,u2) -> new TaxEntryAggregation(u1.count() + u2.count(),
                                                      u1.weightedAveragePrice().add(u2.weightedAveragePrice()), 
                                                      u1.totalPrice().add(u2.totalPrice()))
                                                  ),
                                         u -> new TaxEntryAggregation(u.count(), 
                                                 u.weightedAveragePrice().divide(BigDecimal.valueOf(u.count()),
                                                                                 2, RoundingMode.HALF_DOWN), 
                                                 u.totalPrice())
                                         )
                      )
              ));
```

Explanation:
    Collectors::groupingBy(line 2):
        For the classification function, we create a StateCityGroup record
        For the downstream, we invoke Collectors::mapping(line 3):
            For the first parameter, the mapper that we apply to the input elements transforms the grouped state-city tax records to new TaxEntryAggregation entries that assign the initial count to 1, multiply the rate with price, and set the price (line 3).
            For the downstream, we invoke Collectors::collectingAndThen(line 4), and as we’ll see, this will allow us to apply to the downstream collector a finishing transformation.
                Invoke Collectors::reducing(line 4)
                    Create a default TaxEntryAggregation to cover the cases where there are no downstream elements (line 4).
                    Lambda expression to do the reduction and return a new TaxEntryAggregation that has the aggregations of the fields (line 5, 6 7)
                Perform the finishing transformation calculating the averages using the count calculated in the previous reduction and returning the final TaxEntryAggregation (lines 9, 10, 11).

**This can be easily generalized to solve more complex problems. The path is straightforward: define a record that encapsulates all the fields that 
need to be aggregated, use Collectors::mapping to initialize the records, and then apply Collectors::collectingAndThen to do the reduction and 
final aggregation.**
