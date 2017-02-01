# Java 8 in Action by 

[source code]()

## Chapter 7 Parallel Processing & Performance
  - Iterate can be slow: `Stream.iterate(1L, i -> i + 1` same as usual for loop, then limit and `reduce(0L, Long::sum)`
    - generates boxed objects, which have to be unboxed to numbers before they can be added.
    - is difficult to divide into independent chunks to execute in parallel. the whole list of numbers isn’t available at the beginning of the reduction process, making it
      impossible to efficiently partition the stream in chunks to be processed in parallel.
    -Improving
      `LongStream.rangeClosed(1, n).reduce(0L, Long::sum);` is faster a uses primitives.
  - Mutating state (bad for parallel streams)
    - `Accumulator accumulator = new Accumulator();LongStream.rangeClosed(1, n).forEach(accumulator::add);` or
      `public long total = 0; public void add(long value) { total += value; }`
      **You have a data race on every access of total**
  - Pointers for Parallel
    - limit and findFirst that rely on the order of the elements are expensive in a parallel stream. 
    - Source of stream (supporting random index is fast for partitioning): Good: ArrayList & IntStream.range worst: Stream.iterate & LinkedList
  - Fork/Join eg for summing nos quite involved on p217. **A divinde & conquor eg.**
  -Spliterator (new class in Java 8)
    Like Iterators, Spliterators are used to traverse the elements of a source, but they’re also designed to do this in parallel
    Used an eg of counting words in a sentence. Pretty sure there are some other ways.
    **Convert String into Stream:** What about arrays: `Arrays.stream(array)` or `Stream.of(array)`
    `Stream<Character> stream = IntStream.range(0, SENTENCE.length()).mapToObj(SENTENCE::charAt);`
    
## Chapter 8 Refactoring, testing, and debugging
**I am only going to look at the refactoring with Lambdas to begin with. Return to look at CompleteableFuture after
chapter 13 etc..**

### Refactoring (some good tips)
  - Anon Classes to lambdas
    - `Runnable r2 = () -> System.out.println("Hello World");`
    - NB. **this** in lambda refers to the enclosing class.
    - Lambdas cannot shadow vars from enclosing class
    - Ambiguos Functional Interfaces: Let’s say you’ve declared a functional interface with the same signature as Runnable, here called
      Task (this might occur when you need interface names that are more meaningful in your domain model)
      **Cast (Task) is required to compile**
```java
      interface Task{ public void execute(); }
      doSomething((Task)() -> System.out.println("Danger danger!!"));  
```
  - Use method references rather than lambdas where more than 1 line, as help ledgability
      Use static method refs provided by jdk (summing):
      `int totalCalories = menu.stream().collect(summingInt(Dish::getCalories));`
  - When to use FI?
    - a) Conditional deferred execution
        eg logger.log(Level.FINER, () -> "Problem: " + generateDiagnostic());` Also where && and ||
    - b) Execute around p241
      Especially when commoonly surrounding code with some logic.
      eg. `public interface BufferedReaderProcessor { String ocess() throws IOException;          
      This can then be passed as param in method and called like this: `processFile(BufferedReader b -> b.readline);`
    - Design Patterns
      - Strategy
        Saves creating single method concrete classes. Replace ValidatorStrategy interface with lambdas: 
        'Validator val = new Validator((String s) -> s.matches("regEX");)`
      - Template
        Previously use `abstract void makeCustomerHappy(Customer c);` in abstract parent class.
        now just pass in the diff consumers.
            `public void processCustomer(int id, Consumer<Customer> makeCustomerHappy){
            Customer c = Database.getCustomerWithId(id);
            makeCustomerHappy.accept(c);}`
      - Observer
        Can implement the Observer notify method using lambda. **BUT only when method is small**
        eg. f.registerObserver((String tweet) -> {if(tweet != null && tweet.contains("money")){System.out.println("Breaking news in NY! " + tweet);}`
      - Chain of Responsability
        This is a very functional approach. Normally implemented by defining a succesor to a process.
        With lambda we compose functions:
        `UnaryOperator<String> fn1 = (String text) -> text.replaceALl("a", "b");`  //... and another fn2
        `Function<String, String> pipeline = f1.andThen(f2); pipeline.apply("Some text");
        - Factory
          Normally use a switch statement to determine sub class to create. Use a HasMap & lambdas instead.
``` java
          final static Map<String, Supplier<Product>> map = new HashMap<>();
          static {map.put("loan", Loan::new); ...} then Supplier<Product> p = map.get(name); return p.get();
```
      
