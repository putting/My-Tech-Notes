# Collectors

## CollectingAndThen
Allows performing another action on a result straight after collecting ends.

```List<String> result = givenList.stream().collect(collectingAndThen(toList(), ImmutableList::copyOf))```

### 3 egs of using different approaches.
Nested GroupingBy, plus CollectingAndThen with mapping

``` java
Map<Integer, Map<Integer, Long>> map = bids.stream().collect(
        groupingBy(Bid::getBidderUserId,
                groupingBy(Bid::getAuctionId, counting())));

Map<Integer, Integer> map = bids.stream().collect(
        groupingBy(
                Bid::getBidderUserId,
                collectingAndThen(
                        groupingBy(Bid::getAuctionId, counting()),
                        Map::size)));

Map<Integer, Integer> map = bids.stream().collect(
        groupingBy(
                Bid::getBidderUserId,
                collectingAndThen(
                        mapping(Bid::getAuctionId, toSet()),
                        Set::size)));
```
