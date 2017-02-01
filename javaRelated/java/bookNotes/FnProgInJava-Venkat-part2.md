# Functional Programming in Java - Harnessing ... Notes

## Chapter 6 Lazy

### How java applies lazy
                - In logical && and || operations
                - BUT with methods including method params evaluates EAGER.
                                Lambdas are evaluated when created BUT invocation delayed.
                                Solution: Although slightly more long-winded forto call `lazyEvaluator(() -> evaluate(1), () -> evaluate(2));`
                                - Logically && Lambdas. Only first will get evaluated
``` java
                                public static void lazyEvaluator(Supplier<Boolean> input1, Supplier<Boolean> input2) {
                                System.out.println("accept?: " + (input1.get() && input2.get()));
                                public static boolean evaluate(final int value) {
                                                System.out.println("evaluating ..." + value);
                                                simulateTimeConsumingOp(2000);
                                                return value > 100;
                                                }
```
                - Streams. 
                                Think how intermediate ops do NOT run until terminal op reached AND then processes 1 item at a time.
                                This way findFirst() would only mean a few items evaluated.
                - Infinite Streams
                                Defined BUT only executed with limit. nice eg on creating prime lists starting with a seed.
                                NB. How first method call is instance with params `primeAfter(fromNumber - 1)`
                                                BUT 2nd call `Primes::primeAfter` is method ref no params **as they are passed from the stream**
                                `Stream.iterate(primeAfter(fromNumber - 1), Primes::primeAfter).limit(count).collect(Collectors.<Integer>toList());`
                - IsPrime. see p119 for good egs on primes
                                `number > 1 && IntStream.rangeClosed(2, (int) Math.sqrt(number)).noneMatch(divisor -> number % divisor == 0);`
                                
## Chapter 7 Optimizing Recurssion (re-read - advanced with good ideas)
                - Definitely helps to debug source code.
                Various applications employ recursion, such as for finding the **shortest distances on a map**, computing 
                minimum cost or maximum profit, or reducing waste.
                - Tail Recursion
                                Discusses how the idea is to avoid keeping state while wait for result of next recursive call.
                                So a) keep an accumulator
                                BUT **and this solution (although generic) gets VERY COMPLICATED**
                                Will need to read p127 onwards again as has some interesting principals.
                                Uses FI with defaults (to end recursion). Returns FI's for lazy eval etc....
                                V important to understand the following:
``` java
                            //What happens is that the 1st statement ONLY returns 1 Fn.
                                        TailCall<Integer> tailFn2 = factorialTailRec(1, 3);  //So returns a TailCall which is FI
                                        //The invoke runs the stream ONLY once also.
                                        //Its when the stream get is called that the recursive methods are invoked as normal.
                                        //So Stream is executing factorialTailRec(factorial * number, number - 1) by iterate calling TailCall::apply
                                        int result2 = tailFn2.invoke();
                                        //and again the condition = 1 call isDone true method to terminate.
```
                - Memoisation (caching). Speeding up recursive algorithms
                                Discusses find the maximum profit from sales of assets or the shortest route between locations. In an algorithmic
                                technique called **dynamic programming**
                                The code egs are complicated. BUT see the style of how function to store in map via a FN. That is then applied.
                                function param is the actual executed fn. Q. Isn't the map created for each call though?
                                No the calling methos is in loop & ONLY calls the apply not the callMemoized
``` java
                          public static <T, R> R callMemoized(BiFunction<Function<T,R>, T, R> function, T input) {
                                          Function<T, R> memoized = new Function<T, R>() {
                                                          private final Map<T, R> store = new HashMap<>();
                                                          public R apply(final T input) {
                                                                          return store.computeIfAbsent(input, key -> function.apply(this, key));
                                                          }
                                          };
                                          return memoized.apply(input);
                                          }
```

## Chapter 8 Composing with Lambdas

### Functional Prog ideas
                - Immutability. analogy. Ask friend for change of £10. Do not tear it in half.
                                We expect the £10 to disappear and other notes appear in its place. IMMUTABILITY
                                We send **lightweight objects** to functions and expect other objects to emerge.
                                Easier to parallelize.
                - MapReduce (chaining of functions)
                                One op maps element, 2nd combines. Stream.reduce can also have seed.
                                Don't just return a boolean. Return a predicate. OR pass in fn for even more generic fn.
``` java 
                          public static Predicate<StockInfo> isPriceLessThan(final int price) {
                                          return stockInfo -> stockInfo.price.compareTo(BigDecimal.valueOf(price)) < 0;
                          }
                          //Final code. NB reduce Takes BinaryOp. ie Compares 2 items & returns 1
                          symbols
                                          .map(StockUtil::getPrice) //Returns value obj StockInfo
                                          .filter(StockUtil.isPriceLessThan(500))
                                          .reduce(StockUtil::pickHigh)
                                          .get();
 ```
                - Parallel (esp the svc call)
                                We have to distribute the tasks onto multiple threads, then collect the results, then move on to the 
                                sequential steps. While we’re at it, we must ensure there are no race conditions; we don’t want 
                                threads to collide with other threads’ updates and mess up the data.
                                **NB.** The parallel egs here are on collections which we stream and call a service.
                                                What if we wanted the service to stream results? How would that be done. Q Kafka?
                                
## Chapter 9 Bringing it all together
                - a) More Declarative less Imperative
                                `int max = prices.stream().reduce(0, Math::max);` rather than loop and mutate.
                - b) Favor Immutability
                                Shared mutable vars prevent easy parallel. Favor creating lightweight copies in Fn style.
                - c) Reduce Side Effects
                                **Referential transparency**, which means an invocation or a call to a function can be replaced by its result value without
                                affecting a program’s correctness.
                - d) Prefer Expressions Over Statements (dont return anything)
                                Expressions can be composed/chained.
                - e) Design with Higher-Order Functions
                - f) Performance Concerns
                - g) Adopting the Functional Style             

