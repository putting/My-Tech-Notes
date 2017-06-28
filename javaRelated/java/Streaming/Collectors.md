# Collectors

## CollectingAndThen
Allows performing another action on a result straight after collecting ends.

```List<String> result = givenList.stream().collect(collectingAndThen(toList(), ImmutableList::copyOf))```
