# Java 8 in Action

[source code]()

## Chapter 13 Thinking functionally

### Functional vs Declarative
  - Immuatbility
    A method, which modifies neither the state of its enclosing class nor the state of any other objects and returns its entire results using return, is called pure or side-effect free.
    An immutable object is an object that can’t change its state after it’s instantiated so it can’t be affected by the actions of a function.
  - Declarative programming (basis for Functional programmin). What not HOW (no ssignment, conditional branching, and loops)
    eg. `Optional<Transaction> mostExpensive = transactions.stream().max(comparing(Transaction::getValue));`
  - Whats a Function?: method with NO side effects (or at least none exposed to the system).

### Functional Style
  - Our guideline is that to be regarded as functional style, a function or method can mutate only local variables. In addition, objects it references should be immutable. By this we mean all fields are final, and all fields of reference type refer transitively to other immutable objects.
  - To be regarded as functional style, a function or method **shouldn’t throw any exceptions**. USE OPTIONAl instead.
    Might decide to use exceptions internally BUT NOT expose them via public api - use Optional.        
  - Referential transparency:  “no visible side-effects” (no mutating structure visible to callers, no I/O, no exceptions) encode the concept of referential transparency. A function is referentially transparent if it always returns the same result value when called with the same argument value. 

### Functional style in Practice
  - No mutation
    eg. p374 of finding all combinations of a list {1,4,9}. There are 8 including empty list {}
    The approach is very much scala - take 1st element & create **new lists (no mutation)** to concatenate, then recursively call.

### Recursion
    - eg. Factorial: `return n == 1 ? 1 : n * factorialRecursive(n-1);`
      Stream Factorial: `return LongStream.rangeClosed(1, n).reduce(1, (long a, long b) -> a * b);`
    - Tail Recursion eg. `return n == 1 ? acc : factorialHelper(acc * n, n-1);`
    
## Chapter 14 Functional programming techniques

### Functions everywhere
  `Function<String, Integer> strToInt = Integer::parseInt;`
  - Higher-Order
    `Comparator<Apple> c = comparing(Apple::getWeight);` takes a fn and returns a fn.
  - Currying: a technique that can help you modularize functions and reuse code. **Pass a subset of params, return fn**
    `static double converter(double x, double f, double b) {return x * f + b;}` can be made more useful
    `static DoubleUnaryOperator curriedConverter(double f, double b){return (double x) -> x * f + b;}` returning fn
    you can then define lots of diffconversions
      `DoubleUnaryOperator convertCtoF = curriedConverter(9.0/5, 32); DoubleUnaryOperator convertUSDtoGBP = curriedConverter(0.6, 0);`
      and run `double gbp = convertUSDtoGBP.applyAsDouble(1000);`
  - Persistent data structures
    

### Persistent data structures (Immutable ones)
                - Create copies - DO NOT MUTATE
                eg1. Train journeys (via LonkedList) leg A to B, C to D and then MUTATING them. Causes un-intended consequences.
                Functioanl approach CREATE NEW: `return a==null ? b : new TrainJourney(a.price, append(a.onward, b));`
                eg2. Uses a binary tree. The fn approach is costly as has to create copy of entire depth of tree to new node.
### Lazy Evaluation with Streams
                Well, you’ve seen how to place functions inside data structures (because Java 8 allows you to), and these 
                functions can be used to create parts of the data structure on demand instead of when the structure is created.
                - Consuming Streams multiple times
                Streams can't be defined recursively as can only be consumed once.
                Shows an eg of primes, which has problem of consuming stream twice: HEAD & TAIL
                plus infinite recursion in 2nd param of concat below (need to lazy evaluate):
``` java8
                static IntStream primes(IntStream numbers) {
        int head = head(numbers);
        return IntStream.concat(
                IntStream.of(head),
                primes(tail(numbers).filter(n -> n % head != 0))
        );
    }
```            
                - Java lazy lists (similar concept to stream). As a replacement for streams.
                LazyList: Node -Fn> Node -Fn> Node  . **Supplier fns are creating the next node**
                `final Supplier<MyList<T>>> tail`; `MyList<T> tail() { return tail.get();}` 
                Can now create list like this (number are created on demand (NB. Its a recirsive supplier to from):
                `public static LazyList<Integer> from(int n) {return new LazyList<Integer>(n, () -> from(n+1));}`
                
### Pattern Matching
                - Visitor Pattern: Passing a special Visitor object to each of the tree subclasses....
                - Pattern Matching (remember scala) not avail in Java. Avoids case/switch statements.
                Scala does a match and an unwrapping of elements in its match statement.
                **Replacing IF - Makes code more obscure so DON'T DO**
                `static <T> T myIf(boolean b, Supplier<T> truecase, Supplier<T> falsecase) {return b ? truecase.get() : falsecase.get();}`
                can then call `myIf(condition, () -> e1, () -> e2);` Actually param should be Predicate and do pred.apply().
                There solution gets overl complicated for single level pattern matching.
                
## Memoization
                HashMap as a cahce. ConcurrentHasMap for thread safety. numberOfNodes is a hashmap being mutated.
                `return numberOfNodes.computeIfAbsent(range,this::computeNumberOfNodes);`
                
## Chapter 15 Scala
                Good discussion of Scala. Not reading now.
                
## Chapter 16 Summary
                Some good areas discussed in how java should follow scala in static inti for Map, pattern matching etc..
