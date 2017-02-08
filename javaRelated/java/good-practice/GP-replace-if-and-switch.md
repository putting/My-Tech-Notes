# Replacing Nested if and switch statements

Probably the best solution is this (given this sort of code):
``` java
for (int number : numbers) {
    if (number % 2 == 0) {
        int n2 = number * 2;
        if (n2 > 5) {
            System.out.println(n2);
            break;
        }
    }
}
```
  - a) Replace if test with a method.
  - b) You can then use for loops and `if (isEven(n)) l1.add(n); ...`
  - c) **Use  streams for more fluent approach as below:**
``` java
numbers.stream()
            .filter(Lazy::isEven)
            .map(Lazy::doubleIt)
            .filter(Lazy::isGreaterThan5)
            .findFirst()
```

## Interesting first Operator switch type problem
``` java
switch(operation)
case addition  : return add(int a, String b, String c);
case multiply  : return multiply(int a, int b);
case substract : return substract(int a, int b);
```
We have a few options to replace this:
  - Polymorphism. eg `actions.add(addition, new addOperation()); ...`
  - Enum: `ADDITION {public void performOperation(int a, int b) { ...` 
  - Lambdas: If you've a method which has the same signature of your interface you can also pass it to your operation repository like:
 **The solution is a Map and Supplier solution**
``` java
Map<String, IntBinaryOperator> operations = new HashMap<>();
operations.put("add", Integer::sum);
operations.put("subtract", (a, b) -> a - b);
operations.put("multiply", (a, b) -> a * b);
//...
System.out.println(operations.get("multiply").applyAsInt(10, 20));
```

## Use Predicate Parameters to make methodsmore generic
  This doesn't however remove the if ....
  - However if you have a collection, then Stream with predicates
  
## Maybe we can Compose some predicates  

## Might be able to combine Polymorphism (Strategy Pattern)
``` java
interface Strategy{
    boolean accepts(String value);
    void process(String value);
}
//then use this which for each item Streams all the possible strategies, to find the method to process
list.forEach((item) -> {
  strategies.stream()
    .filter((s) -> s.accepts(item)) // find the appropriate strategy
    .findFirst()                    // if we found it
    .ifPresent(strategy -> strategy.process(item)); // apply it
    // or use .getOrElseThrow to fail fast if no strategy can be found
});
```
