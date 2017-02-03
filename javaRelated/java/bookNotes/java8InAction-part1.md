# Java 8 in Action: Overall v good book – interesting to learn from basics again. 

**See LazyLists p398 impl for some v interesting coding approaches**
                Using Lazy Supplier : () -> from(n+1) where n also passed into method. To create the next number
                The call is recursive (infinite lazy list) BUT only appears to get called ONCE (as lazy) then tail does get to end recursion!!!
                **Crucial part:** create parts of the data structure on demand. This case Suppier inside custom MyLinkedList

[https://github.com/java8/Java8InAction](https://github.com/java8/Java8InAction)

## Chapter 2 Passing behaviour with parameters

### Good eg of Strategy for beahviour
                Takes an eg of filtering Apples (by diff criteria). Sep method for kind of filter. Worse || combining tests in method.
                Much better to have single generic filter.
                Before java 8 would define a Predicate Interface & then a child class for each implementation.
                This would be passed to filter method and predicate.test executed.
                **NB.** This is exactly how the lambda & Predicate approach works BUT **no new classes required**.
                eg ApleFormatter. Think how **diff behaviour** is diff methods, defined by client and passed to method as param.

### Execute Arouund Pattern
                eg. using BufferedReader. Because the pattern is open the file, do activity close. 
                Its only the activity that changes. Pass in actvity: `processFile((BufferedReader br) -> br.readLine() + br.readLine());`
                Here 2 lines read, could be x lines or something else.      
                
## Chapter 3 Lambda Expressions

### In-build Functional Interfaces do not throw checked exceptions. You will need to create your own FI for this.
                OR wrap ex and rethrow runtime ex.
                - Special void compatability. even though list.add return boolean, still compatible when void return expected
                                Predicate<String> p = s -> list.add(s);
                                Consumer<String> b = s -> list.add(s);    // Consumer has a void return, BUT still OK.
                - Invalid as Object not FI: `Object o = () -> {System.out.println("Tricky example"); };` make it Runnable
                - Capturing Vars. This is OK as effectively final: `int portNumber = 1337;Runnable r = () -> System.out.println(portNumber);`
                Instance variables are stored on the heap, whereas local variables live on the stack. If a lambda could access the local 
                variable directly and the lambda were used in a thread, then the thread using the lambda could try to access 
                the variable after the thread that allocated the variable had deallocated it.
                **Specifically the issue is: var created in 1 thread, lambda executing in diff thread.** Prob if main thread de-allocates var.

### Method References
                eg. Apple::getWeight. **NB* No param as NOT calling method yet just definig.** like (Apple a) -> a.getWeight()
                It is just a reference to a method called getWeight in the Apple class.
                - Three type of Method Ref:
                                - a) Static method. (args) -> ClassName.staticMethod(args) = ClassName::staticMethod. eg `Integer::parseInt`
                                - b) Instance method of arbritary type. (arg0, rest) -> arg0.instanceMethod(rest) = ClassName::instanceMethod
                                                arg0 is of type ClassName. eg. `(String s) -> s.toUpperCase() can be rewritten as String::toUpperCase`
                                - c) instance method of existing obj. (args) -> expr.instanceMethod(args) = expr::instanceMethod
                                                eg. `() -> varA.getValue()` = `varA::getValue`
                                So the difference with b) & c) is that with b) the instance var does not exist (other than as a param)
                                See here the strings to be compared are in the collection and not pre-defined vars. So this is an eg of b)
                                                `List<String> str = Arrays.asList("a","b","A","B"); str.sort(String::compareToIgnoreCase);`
                - Constructor method refs:
                                With a param needs to be `Function<Integer, Apple> a1 = Apple::new` not Supplier<Apple> which is ok for no arg constructor.
                                As the ref var fn indicates a param to be passed in when applied. `Apple a2 = a1.apply(110)`
                                A 2 arg constructor would be BiFunction<String, Integer, Apple>
                                The ability to associate constructor to a ref var without creating it can be useful:
                                                Map<String, Function<Integer, Fruit>> map... map.put("apple", Apple::new); map.put("orange", Orange::new) etc..
                                No TriFunction<T, U, V, R> so can create your own
                - Comparator.comparing static method
                                `import static java.util.Comparator.comparing; inventory.sort(comparing(Apple::getWeight));   `
                                They can be chained .reversed().thenComparing(Apple::getCountry) 
                                Same is true for predicates.
                - Fn.diff between andThen and compose
                                f.andThen(g) applies f first and then g
                                f.compose(g) applies g first and then f
                                
## Chapter 4 Introducing Streams            
                - .map interestingly is creating a new version of rather than modifying.
                - `words.stream().map(word -> word.split(""))...` returns Stream<String[]>. *NB.* intermediate ops return Stream<T>
                                resolved by:
                                - a) `.map(word -> word.split("")).map(Arrays::stream)..` DOES NOT WORK creates `Stream<Stream<String>>`
                                - b) flatMap(Arrays::stream). mapping not with a stream but with the contents of that stream.  

### Stream vs Collection
                - A collection is an **in-memory** data structure that holds all the values the data structure currently has.
                                You can add and delete from.
                - A Stream fixed data structure (you can’t add or remove elements from it) whose elements are computed on demand.
                                This is a form of a producer-consumer relationship. Another view is that a stream is like a lazily constructed collection.
                                
## Chapter 5 Working with Streams

### Embedded For-Loop eg. for (x x < 10) for (y < 5) ..etc
                - The Stream equivalent is to stream first . eg taking 2 lists and finding [x,y] pairs from them
                                `numbers1.stream().flatMap(i -> numbers2.stream().map(j -> new int[]{i, j}))`
                                Streaming the first list, then for each element Stream second, so we have both values i and j
                - Short-circuiting
                                allMatch, noneMatch, findFirst, and findAny don’t need to process the whole stream to produce a result.
                                
### Reduce
                - int sum = `numbers.stream().reduce(0, Integer::sum);`
                - or optional if no initial value `Optional<Integer> sum = numbers.stream().reduce((a, b) -> (a + b));`
                - using method ref: Optional<Integer> max = numbers.stream().reduce(Integer::max);
                - Counting using map & reduce
                - **BE CAREFUL** reduce("", (s1, s2) -> s1 + s2) is inefficient as concatenates new strings every time.
                  Collectors.joining uses a StringBuilder internally is much more efficient.
                
### stateless vs. stateful
                - Operations like reduce, sum, and max need to have internal state to accumulate the result.
                                Here the internal state is of **bounded size** no matter how many elements are in the stream being processed.
                - Sorted or distinct require knowing the **previous history** to do their job.
                                sorting requires **all the elements to be buffered** before a single item can be added to the output stream; 
                                the storage requirement of the operation is **unbounded**.
                                
### Stream Examples
  - I used fileter and findAny for any traders based in Milan to return boolean.
    Could use .anyMatch(t -> t.getTrader().getCity().equals("Milan")) which is similar.
  - Max. I sued mapToInt then .max, whereas they used map then .reduce(Integer::max) or could have 
  manually done `reduce((t1, t2) -> t1.getValue < t2.getValue() ? t1 : t2);`
  - Min. Also possible to do `stream.min(comparing(Transaction::GetValue))`

### Creating streams
  - Stream<String> stream = Stream.of("Java 8 ", "Lambdas ", "In ", "Action");
  - int[] numbers = {1,2,3}; Arrays.stream().sum();
  - try(Stream<String> lines = Files.lines(path, charset)...) catch IOException
  - Streams from Functions & Infinite Streams `Stream.iterate(0, n -> n + 2).limit(10).forEach(System.out::println);`
    In general, you should use iterate when you need to produce a sequence of successive values, for example, a date followed by its next date

## Chapter 6 Collecting with Streams
TODO: Lots of good stuff on grouping and partitioning.


