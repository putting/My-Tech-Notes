# Functional Programming
The purpose of these serious of questions is to test whether the candidate has an undertsanding of the principles behind FP. 
Especially as java 8, scala and js all have varying degress of FP.

## Links
[Very good overview with some egs.](https://medium.com/javascript-scene/master-the-javascript-interview-what-is-functional-programming-7f218c68b3a0)

## Introduction
Functional programming (often abbreviated FP) is the process of building software by composing **pure functions**, avoiding 
**shared state**, **mutable data**, and **side-effects**. Functional programming is declarative rather than imperative, and 
application state flows through pure functions. 
Contrast with object oriented programming, where application state is usually shared and colocated with methods in objects.

## Summary
  - Pure functions: Same result with ref integrity as no external side-effects and takes only params.
  - Function composition: Combining 2 or more fns.
  - Avoid shared state: Race conditions etc..
  - Avoid mutating state: Hard to test and reason about requiring knowledge of var history. Mutation is lossy, whereas immutable leaves a trail
    - Follow up quest. const or final ref in java do NOT mean the object cannot be mutated.
  - Avoid side effects: Makes prog easier to understand. Writing to file, console, external process etc.. Haskel uses **mondas**. 
    Although unavoidable at times, should be isolated from rest of prog logic.
  - Reusability via Higher Order Functions. See below
  - Functors (comtainers like List, Stream and Array). Something that can be **mapped** over. The concept of using abstractions like functors & higher order functions in order to use generic utility functions to 
  manipulate any number of different data types is important in functional programming
  
  
### Higher Order Functions
Functional programming tends to reuse a common set of functional utilities to process data. Object oriented programming tends to 
colocate methods and data in objects. Those colocated methods can only operate on the type of data they were designed to operate 
on, and often only the data contained in that specific object instance.
In functional programming, any type of data is fair game. The same map() utility can map over objects, strings, numbers, or any 
other data type because it takes a function as an argument which appropriately handles the given data type. FP pulls off its 
generic utility trickery using higher order functions.

JavaScript has first class functions, which allows us to treat functions as data — assign them to variables, pass them to other 
functions, return them from functions, etc…

A higher order function is any function which takes a function as an argument, returns a function, or both. Higher order 
functions are often used to:

Abstract or isolate actions, effects, or async flow control using callback functions, promises, monads, etc…
Create utilities which can act on a wide variety of data types
Partially apply a function to its arguments or create a curried function for the purpose of reuse or function composition
Take a list of functions and return some composition of those input functions

