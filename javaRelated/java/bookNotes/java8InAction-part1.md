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

### Refactoring
