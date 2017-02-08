# Guidelines for Lambdas

[http://www.baeldung.com/java-8-lambda-expressions-tips](http://www.baeldung.com/java-8-lambda-expressions-tips)

## Summary

Example used:
``` java
@FunctionalInterface
public interface Foo {
    String method();
    default void defaultMethod() {}
}
```


  - Prefer Standard Functional Interfaces
    ie. The built in ones `public String add(String string, Function<String, String> fn) {..`
  - Don’t Overuse Default Methods in Functional Interfaces
    FIs can extend interfaces as normal too
      `@FunctionalInterface public interface FooExtended extends Baz, Bar {}`
    Adding too many default methods to the interface is not a very good architectural decision.
  - Instantiate Functional Interfaces with Lambda Expressions
    Do this `Foo foo = parameter -> parameter + " from Foo";` rather than instantiate like anon class.
  - Avoid Overloading Methods with Functional Interfaces as Parameters
    Not good practice. Best to use diff method names. Implementing class will get: reference to add is ambiguous both method
``` java
  public interface Adder {
      String add(Function<String, String> f);
      void add(Consumer<Integer> f);
  }
```
  - Don’t Treat Lambda Expressions as Inner Classes
    `this` refers to enclosing class, rather than to inner class.
  - Keep Lambda Expressions Short And Self-explanatory
    - Avoid Blocks of Code in Lambda’s Body
      When more than a few lines. Create a method and refer to it in lambda. `Foo foo = parameter -> buildString(parameter);`
      Q. Check this matches Foo interface as metjhod name diff?
    - Avoid Specifying Parameter Types
      eg `(a, b) -> a.toLowerCase() + b.toLowerCase();`
    - Avoid Parentheses Around a Single Parameter
      eg. `a -> a.toLowerCase();`
    - Avoid Return Statement and Braces
      eg. `a -> a.toLowerCase();`
    - Use Method References
      instead of above `String::toLowerCase;`
    - Use “Effectively Final” Variables
      Accessing a non-final variable inside lambda expressions will cause the compile-time error. But it doesn’t mean that you should mark every target variable as final.
    - Protect Object Variables from Mutation
      The “effectively final” paradigm helps a lot here, but not in every case. 
      **Lambdas can’t change a value of an object from enclosing scope.**
      But in the case of mutable object variables, a state could be changed inside lambda expressions.
``` java
int[] total = new int[1];
Runnable r = () -> total[0]++;
r.run();
```
    This code is legal, as total variable remains “effectively final”. But will the object it references to have the same state after execution of the lambda? No!
    Keep this example as a reminder to avoid code that can cause unexpected mutations.
