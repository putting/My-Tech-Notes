# Java 8 Lambdas Book - Interesting code. 

## Chapter 3

### Advanced Questions
                - `FilterUsingReduce`: Shows how to accumulate lists then **combine** them.
                - `MapUsingReduce`: Like the way a Mapper is passed into method. Again combined.
                
## Chapter 5
                - Custom Collectors can be useful for doing generic string combining etc.. see p73 for an eg.
                                Its v interesting to see that a `Collector` is a Supplier, Add(accumulate), Merge(reduce) and finisher functions combined.
                - on p75 need to understand this: This is especially useful if you have a domain class that you want 
                to build up from an operation on a collection and none of the standard collectors will build it for you.
                see p76 discussion as Collectorts are kind of Builders..and into your own Domain class constructor from a collection.
                eg. Files.lines() returns a stream of lines and you want to create domain objects from them.
                
## Chapter 8
                                - Well worth looking at the replcaement Design Patterns. Template is confusing - investigate further.
                                - DSL's (external egs CSS & regex). Internal are in prog language such as Mockito or SQLBuilder api.
                                                Good for **fluent** api's.          Used as Libraries/Frameworks. eg to help testing be more fluent.
                                                And describe a set of syntax rules for using them.
                                - SOLID
                                                - SRP: eg BalanceSheet. Split into Tabulate & Render classes. Also methods should be SR.
                                                                How would I do this? Compose BS of these 2 new classes I guess.
                                                                Also **cohesion** Methods & Fields treated together as closely related.
                                                                eg Counting primes. removing loops.
                                                                `isPrime(int number) {return IntStream.range(2, number).allMatch(x -> (number % x) != 0);`
                                                - Open/Closed Principle (open for extending beahviour. closed to changing state/existing beahviour)
                                                                Use an abstraction to plug in new functionallity. 
                                                                see eg p131 of passing in obj rather multiple methods in api.
                                                                                api{ setXval(); setYval(); setZval()} replace with setVal(ValObject)
                                                                                so we can replace ValObject with multiple diff impls.
                                                                **lambda: eg again ThreadLocal.withInitial passing lambda to change behavior**
                                                - DI: p136 shows a bit of a complicated eg, where lines lambda is passed in.
                                                                Definitely worth looking at the concept again.
                                                                The method withLinesOf then becomes a generic file reader/handler. The fn can be injected.
                                                                
## Chapter 9
                Solution is the same: use lambda expressions to represent the behavior and build APIs that 
                manage the concurrency for you.
                - Callbacks: Should the user obj on p141 really have sockets and Veritcle params? This is more a UserHandler than User class.
                - Event Bus (in-process by use of Vert.x). Pub-Sub possible too.
                - Callback pyramid of doom. Some re-facoring but FUTURES proposed. or rather 
                - CompletableFuture: thenCompose & thenCombine egs.
                
