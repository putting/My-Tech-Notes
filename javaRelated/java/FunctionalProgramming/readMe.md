# Functional Programming

## This is a really Good description (@cscalfani on medium)
(https://medium.com/@cscalfani/so-you-want-to-be-a-functional-programmer-part-1-1f15e387e536)
- Part 1: Pure functions, Immutability and recursion
- Part 2: Fn vars, Higher-Order fns, Closures
- Part 3: Functional Composition, **NB Can't compose fns with diff amount od params - SO currying**
- Part 4: Currying and Common functional fns (Map, filter, Reduce) which reduce boilerplate code and pass fns as params.
- Part 5: Ref Transparency (replace fn with iyts expr), Exec Order (pure fns allow this), Types (inference is good)
- Part 6: Implementing
  - Immutability: Use const in js (although obj from the ref can still be mutated). Can use Immutable.js, but makes code more like Java
  - Currying & Comosition: currying allows template fn's with a param be created. Then further specialisms can be created.
  lib **Rambda** allows R.curry((a,b)=>a+b
  - Conclusion Proposes **Elm** which is pure functional lang rather than js.

## Taken from Functional Programming in Java

### Currying
There is no such thing as a function with multiple parameters. Although it can have an input of a Tuple (of varying sizes).
So when you apply a fn to a Tuple, rather than inputs say all ints, they are fn(ints) i.e first fn returns a fn and so one for all inputs.
eg. f(x)(y) is the curried form of the function f(x, y)
This leads to use use of **partially applied fns**.
Where say one input is know and applied to fn. This can act as a template eg a FxRate fn to be used later as part of xrate conv. Normally you define the template where the input is fairly static such at apply9pcntRate fn.

### Pure Functions
- Suprisingly this is a Pure Fn as dependent on inst var percent1. **As long as only accessed once in method (twice you need to take a local copy - as it is evaluated outside method and may change)**.
NB. applyTax is a fn of **Tuple(a, percent1)** percent1 is seen as an implicit arg
Despite using
```java
public int percent1 = 5;  //making final would be better though!
public int applyTax1(int a) {
return a / 100 * (100 + percent1);
}
```

### Composing Functions
Think of composing fns as appying methods: square.apply(triple.apply(2)), where triple resolves to an int which is passed into square.
**This isnt fn comp** but functional applications.
**Functional Comp is a Binary op on fns**. Just like addition is on binary nos. eg applied to each other.
```java
Function compose(final Function f1, final Function f2) {
return new Function() {
@Override
public int apply(int arg) {
return f1.apply(f2.apply(arg));}};}
System.out.println(compose(triple, square).apply(3));
27
```

#### Question (TODO answer)
why do you need function objects? Couldn’t you simply use methods?


## Redux
Full decription of Redux [start here](https://medium.com/javascript-scene/10-tips-for-better-redux-architecture-69250425af44)
Full video course here(https://egghead.io/courses/getting-started-with-redux)
and building idiomatic.. (https://egghead.io/courses/building-react-applications-with-idiomatic-redux)
