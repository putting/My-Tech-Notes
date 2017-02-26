# Fibonacci

Fibonacci = (0) 1 1 2 3 5 8 13 21 44
[https://dzone.com/articles/do-it-java-8-recursive-and](https://dzone.com/articles/do-it-java-8-recursive-and)
 
NB. BigInteger rather than int can be used for Arithmetic overflow

## Recursive
``` java
public int fibonacciRecursive(int number) {
    if (number == 1 || number == 2) {
        return 1;
    }
    return fibonacciRecursive(number - 1) + fibonacciRecursive(number - 2);
}
``` 
## Tail Recursive

``` java
public static BigInteger fibTail(int x) {
    return fibTailHelper(BigInteger.ONE, BigInteger.ZERO, BigInteger.valueOf(x));
}
//Seams complicated
public static BigInteger fibTailHelper(BigInteger acc1, BigInteger acc2, BigInteger x) {
    if (x.equals(BigInteger.ZERO)) {
        return BigInteger.ONE;
    } else if (x.equals(BigInteger.ONE)) {
        return acc1.add(acc2);
    } else {
        return fibTailHelper(acc2, acc1.add(acc2), x.subtract(BigInteger.ONE));
    }
}
```
## Using Streams
``` java
public void testFibonacciStreamNonRecursive(int nth) {
    //Due to the lack of a standard pair type, it uses a two-element array.
    Stream.iterate(new long[]{1, 1}, p -> new long[]{p[1], p[0] + p[1]})
            .limit(nth)
            .forEach(p->System.out.println(p[0]));
}
```
## Using Tuple
``` java
class Tuple<T, U> {
        public final T _1;
        public final U _2;
        public Tuple(T t, U u) {
            this._1 = t;
            this._2 = u;
        }
    }

//Is the Tuple the equivalent of the array above
public void usingCoolTuple(int number) {

    Tuple<BigInteger, BigInteger> seed = new Tuple<>(BigInteger.ONE, BigInteger.ONE);
    UnaryOperator<Tuple<BigInteger, BigInteger>> f = x -> new Tuple<>(x._2, x._1.add(x._2)); //Creates new value
    Stream<BigInteger> fiboStream = Stream.iterate(seed, f)
            .map(x -> x._1)
            .limit(number);

    System.out.println("cool tuple:" );
    fiboStream.forEach(System.out::println);
}
``` 
