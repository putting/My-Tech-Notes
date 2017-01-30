# Good Practices & Coding Hints: EMAILED 30/1/2017

## Objects.  ? is this google null stuff
Ojava.util.Objects.requireNonNull(param var) //Makes sure invariants are kept. Otherwise use Optional.

## Public Api Best Practices
                - Returning Parent not Impl each time. find eg of this.

## Hashing & Equals

## String manipulations
                - Use the stream collectors joining fn: `.collect(Collectors.joining(", ", "[", "]"));`
                - `java.util.StringJoiner`

## Streams
                - Pairs, tuplets etc... `Stream.of(obj.getName(), obj.getNationality()))` Neat way of generating.
                - ### Replace Ternary Operator with max. eg `stream.max(Comparator.comparing(StringExercises::countLowercaseLetters))`.
                                Use this as opposd to reduce if then approach as realy is Comparing.
                - Getting a member and child member and **concatenatin**
                                `.flatMap(artist -> concat(Stream.of(artist), artist.getMembers()))`

## Stream Custom Collectors (NB. There are lots of uses for this, including reduction/max etc..)
                - Thought of an eg. Files.lines() returns a stream of lines and you want to create domain objects from them for each line.
                - Collectors let us compute the final values of streams and are the mutable analogue of the reduce method.
                - Counting occurences of items: `names.collect(groupingBy(name -> name, counting()))`
                - see p86 Java 8 lambdas for **Monte Carlo simulation** of dice
                - Note type syntax `.collect(Collectors.<String>toList());`


## Method Return Streams rather than Lists etc..
                - Includes Domain objs. Allows chaining & lazy execution. eg. album.getMusicians().filter(...).map(..)
                - **BIG WIN:** Streams are immutable too, so no need to make defensive copies.
                - If can't change Domain method then just call .stream on returned List

## HashMaps & Lambdas
                - computeIfAbsent/putIfAbsent **ARE ATOMIC & SO THREADSAFE**. ie will check and update within a lock.
                                So, should also see other threads updates too.
                - putting: `artistCache.computeIfAbsent(name, this::readArtistFromDB);`
                - iterating over: `albumsByArtist.forEach((artist, albums) -> {countOfAlbums.put(artist, albums.size());`
                
## Replacing if and switch with lambdas
                - track down email.

## Parallel Streams
                - Under the hood use the Fork/Join framework to divide up elements os stream.
                - Random access collections such are ArrayList and IntSream.range are good as can be SPLIT easily for for/join.
                - Stateful ops are slower: sorted, distinct and limit. (stateless map, filter, flatMap) reduce?

## New Array helpful functions
                - Moving avg & running totals etc... NB see p93: `Arrays.parallelSetAll(values, i -> i);`
                - Copy array: `double[] sums = Arrays.copyOf(values, values.length);`
                
## Algorithm solutions
                - Fiboacci with caching: `cache.computeIfAbsent(x, n -> fibonacci(n-1) + fibonacci(n-2));`
                                Pre-populate with 0 and 1
                
## Threading & lambdas
                - **Amdahl’s Law**: theoretical maximum speedup of a program on a machine with multiple cores.
                                If we take a program that is entirely serial and **parallelize only half of it**, then the maximum 
                                speedup possible, regardless of how many cores we throw at the problem, is 2×.
                                In this eg the ramaing half left in serial may dominate performance of app.
                - Roughly collections > 1000 items can benefit from parallel.
                - awaitCompletion. **p87** is a get on each Future in List. Sounds like CompletableFuture?
                                ALSO see how makeJob returns a Runnable. I think this is a good way to Build tasks.

## ThreadLocal
                - ThreadLocalRandom.current();. see p87 for interesting random generator in threads. makeJob()

## CompletableFuture
                NB. Assigning a result to a CF does not mean it us yet executed. 
                .get() executes. Check whether executes BUT result not know or waits for termination method call.
                - join acts as a terminator (can use lambda to combine 2 result sets). see p149
                - `CompletableFuture.supplyAsync(() -> {`
                - **ch9 exercise. NB** 
                                - The call to a svc remains same. returns long NOT Future (ie not composing this method)
                                - With 2 svc calls exec like this:
                                                - Exec async 1st call: 
                                                `CF<Long> otherArtistMemberCount = CompletableFuture.supplyAsync(() -> getNumberOfMembers(otherArtistName));`
                                                - 2nd call uses diff call: 
                                                `CF<Long> artistMemberCount = CompletableFuture.completedFuture(getNumberOfMembers(artistName));`
                                                - then 2 CF's combined and boolean condition `artistMemberCount.thenCombine(otherArtistMemberCount, (count, otherCount) -> count > otherCount)
                         .thenAccept(handler::accept);`
                                                - thenAccept IS THE CALLBACK (Consumer).
                                                - to continue chaining would need to do .thenApply which takes a Fn instead of Consumer.
                                                - .thenCompose is a flattening version of thenApply.

## Interfaces in java 8
                - Use of default methods in tests
                - Use Lambdas impls instead of concrete classes
                - public static final variables here
                - ?? There are some good other uses I have seen
                - Default methods on interfaces DO NOT have vars, so can only chnage child implementation vars via their methods.
                - **Static Methods:** Can be v useful for Builders / class specific utility methods.
                - Default methods can call abstract methods in interface. eg. getMusuicians().stream etc..

## Abstract classes vs Interfaces               
                Interfaces give you multiple inheritance but no fields, while abstract classes let you
                inherit fields but you don’t get multiple inheritance.

##Testing & Frameworks
                - Lambdas can be tricky - **use method Ref instead**. It is sometimes better to create a normal method & in stream.
                                esp multi-line lambdas. **IMP: @Test can then call method directly instead of Lambda** see p104
                - Test doubles : Mocks & Stubs. Mocks allow you to verify behaviour.
                - otherList is concrete list to verify Mocked List `when(list.size()).thenAnswer(inv -> otherList.size());`
                                nb Mockito allows thenAnswer takes fnal interface.
                - peek() can also be used for logging too as well as for setting breakpoints.

## Design patterns using Lambdas (thes egs are simple. maybe diff with more complex egs.)
                **Overriding common learning poinbt is that Lambdas end up replacing MULTIPLE impls of an Interface.**
                Thats where behaviour is slightly dif for each child. BUT what if you want those types?
                - Command Pattern: Sequencing methods based on runtime decisions.
                                see p111. Commands impls with single methods - replace with lambdas.
                - Strategy Pattern: Changing algorithmic beahviour at runtime
                                eg CompressionStrategy. Compressor (is the context)
                - Observer Pattern
                                The action to be taken on Observer when state changes on Observable can be a lambda.
                                eg. `name -> {if (name.contains("Apollo"))System.out.println("We made it!");`
                Template Pattern
                                Not sure I follow exactly on p122, but replacing inheritance with composition.
                                
##Benchmarking
                - jmh: Allows you to annotate a class: `@BenchmarkMode(Mode.AverageTime)` and around method `@GenerateMicroBenchmark`
                
## Optimisation
                - Make collection arrayList, then convert Integer to int for sum rather than reduce.
                                eg. `arrayListOfNumbers.parallelStream().mapToInt(x -> x * x).sum();`

## Refactoring to Lambdas
                - If you find that your code is repeatedly querying and operating on an object only to push a value 
                                back into that object at the end, then that code belongs in the class of the object that you’re modifying.
                                The eg given id the logger.isDebugEnabled logger.debug(); should be easily solved by passing in code as data:
                                                logger.debug(() -> "msg:" + expensiveMethod(). ie **the logger decides**
                - Create child to override single method.
                                
##Logging           
                - `logger.debug("Look at this: " + expensiveOperation());` still requires expensiveOp to be called even if **NOT** as debug level
                                Can create a method which takes lambda `debug(Supplier<String> message)` which defers lazy init cost if not debug level.see p42 Java 8 Lambdas
                                
                
## Properties loading & getting resource
                Properties props = new Properties();
    props.load(this.getClass().getResourceAsStream("/file.properties"));
               
