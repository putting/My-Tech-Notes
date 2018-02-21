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

