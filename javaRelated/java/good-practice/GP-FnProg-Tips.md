# Assorted Good Practice for Functional Style Programming

# Functional Programming Code snippets / good practice

## Replace if's with Predicate params
                - This avoids creating similar behaviour methjods based on diff if'same
                                `public int sumAll(List<Integer> numbers, Predicate<Integer> p) {....if (p.test(number)) {.....`

## Replace nested ifs & try-catch blocks                                
[https://dzone.com/articles/why-we-need-lambda-expressions-0](https://dzone.com/articles/why-we-need-lambda-expressions-0)

                - Nested If. Can be replaced by separate methods BUT better to replace by stream.filter.map.filter.findFirst()
                - eg disposing of Resource requires try-finally **which is a pattern repeated often**. 
                                create a static method `withResource(Consumer<Resource> consumer) { Resource resource = new Resource(); try {consumer.accept(resource);} finally {resource.dispose();}}`
                                This allows you to call the method like this: `withResource(resource -> resource.operate());`
                                **NB PATTERN. A Consumer is just an obj paramater which you call a method on.**
                                I think I have seen this where lambda nested in lambda - and the param is taken from the 1st lambda.
                                                This is also a Consumer in forEach: `numbers.forEach((Integer value) -> System.out.println(value));`
                                                
## Functional Style          
                - Composing fns esp Single Methods. Use factory method. eg `ThreadLocal.withInitial(() -> database.lookupCurrentAlbum());`
                                also true for simple Runnable
                - **See p100 for v good eg** of breaking down where returning multiple related counts from a Domain obj.
                                Albums - track running time, count musisicans, tracks : Similar related counts.
                                eg composed fn `countFeature(ToLongFunction<Album> function)` so we can now pass in different fns.                                                

## Comparator
                - Good idea to define comparators for re-use. eg. `Comparator<Artist> byNameLength = comparing(artist -> artist.getName().length());`

## Method References
                - Use in streams for instantiating. `.orElseThrow(RuntimeException::new)`

## Callback with Lambdas
                - see p140 using vert.x. Provides lambda on connection and also data Handler class.
                                interesting implementation
                - `Pattern.compile("\\n").splitAsStream(string)`                
                
## Make methods re-usable.
                - [code here](https://dzone.com/articles/functional-programming-in-java-8-part-1-functions-as-objects)
                - final eg: `compute((a) -> -a, toInvert);` where method is `static Integer compute(Function<Integer, Integer> function, Integer value)`
