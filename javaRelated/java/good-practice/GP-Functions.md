# Good Practice with Lambdas & Functions

[http://www.baeldung.com/java-8-functional-interfaces](http://www.baeldung.com/java-8-functional-interfaces)

There are lkots of good code egs here

## Primitive Function Specializations
  - There is no fn, so could define as follows:
``` java
@FunctionalInterface
public interface ShortToByteFunction {
    byte applyAsByte(short s);
}
//A method that takes this FI
public byte[] transformArray(short[] array, ShortToByteFunction function) {
    byte[] transformedArray = new byte[array.length];
    for (int i = 0; i < array.length; i++) {
        transformedArray[i] = function.applyAsByte(array[i]);
    }
    return transformedArray;
}
//Could then be used
short[] array = {(short) 1, (short) 2, (short) 3};
byte[] transformedArray = transformArray(array, s -> (byte) (s * 2));
 
byte[] expectedArray = {(byte) 2, (byte) 4, (byte) 6};
assertArrayEquals(expectedArray, transformedArray);
```

## Two-Arity Function Specializations
  - BiFunctions. Used to replace map values
  `Map<String, Integer> salaries = new HashMap<>(); salaries.put("Freddy", 30000);
  salaries.replaceAll((name, oldValue) -> name.equals("Freddy") ? oldValue : oldValue + 10000);`

## Suppliers
This allows us to lazily generate the argument for invocation of this function using a Supplier implementation. 
This can be useful if the generation of this argument takes a considerable amount of time.
``` java
public double squareLazy(Supplier<Double> lazyValue) {
    return Math.pow(lazyValue.get(), 2);
}
Supplier<Double> lazyValue = () -> {
    Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);
    return 9d;
};
Double valueSquared = squareLazy(lazyValue);
//Another use case for the Supplier is defining a logic for sequence generation
int[] fibs = {0, 1};
Stream<Integer> fibonacci = Stream.generate(() -> {
    int result = fibs[1];
    int fib3 = fibs[0] + fibs[1];
    fibs[0] = fibs[1];
    fibs[1] = fib3;
    return result;
});
```
  The function that is passed to the Stream.generate method implements the Supplier functional interface. Notice that to be useful as a generator, the Supplier usually needs some sort of external state. In this case, its state is comprised of two last Fibonacci sequence numbers.
  To implement this state, we use an array instead of a couple of variables, because all external variables used inside the lambda have to be effectively final.

## Consumers
``` java
List<String> names = Arrays.asList("John", "Freddy", "Samuel");
names.forEach(name -> System.out.println("Hello, " + name));
//or BiConsumer
Map<String, Integer> ages = new HashMap<>();
ages.put("John", 25);
ages.put("Freddy", 24);
 
ages.forEach((name, age) -> System.out.println(name + " is " + age + " years old"));
```
## Composing using Lambda Fns
  - Creating a new Fn from 2 others before applying.
``` java 
Function<Integer, String> intToString = Object::toString;
Function<String, String> quote = s -> "'" + s + "'";
Function<Integer, String> quoteIntToString = quote.compose(intToString);
assertEquals("'5'", quoteIntToString.apply(5));
```

## Predicates
  `List<String> namesWithA = names.stream().filter(name -> name.startsWith("A"))`
  
## Operators
   Receive and return the same value type
   `names.replaceAll(String::toUpperCase);`
   or BinaryOperator reduction
   `int sum = values.stream().reduce(0, (i1, i2) -> i1 + i2);`
   The reduce method receives an initial accumulator value and a BinaryOperator function. 
   The arguments of this function are a pair of values of the same type, and a function itself contains a logic for joining them 
   in a single value of the same type. Passed function must be associative,    which means that the order of value aggregation 
   does not matter, i.e. the following condition should hold:
  `op.apply(a, op.apply(b, c)) == op.apply(op.apply(a, b), c)`
  
  ## Legacy Functional Interfaces
    - Runnable, Callable & Comparator
    `Thread thread = new Thread(() -> System.out.println("Hello From Another Thread")); thread.start();`
