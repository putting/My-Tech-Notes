# Functional Programming in Java Book

## Chapter 3 : Making Java more Functional

### Email verifier eg
- Replace method with fn.
- Replace return Boolean with a Success/Failure class
- Fix validate method below to be more functional
  - Make method return something that can be executed. Here they define ```public interface Executable {void exec();}```

#### Can't test methods which return void. Has side-effects, mutates outside world.
Plus uses instanceof and has a cast. Bad!
```java
static void validate(String s) {
  Result result = emailChecker.apply(s);
  if (result instanceof Result.Success) {
    sendVerificationMail(s);
  } else {
    logError(((Result.Failure) result).getMessage());
  }
}
```
so becomes (and requires further clean ups..):
The validate method now returns Executable instead of void. It no longer has any side effect, and it’s a pure function. When an 
Executable is returned B, it can be executed by calling its exec method. Note that the Executable could also be passed to other methods 
or stored away to be executed later. In particular, it could be put in a data structure and executed in Executables are executed by 
calling exec() Method validate now returns a value and has no side effect sequence after all computations are done. 
This allows you to separate the functional part of the program from the part that mutates the environment.
```java
static Executable validate(String s) {
  Result result = emailChecker.apply(s);
  return (result instanceof Result.Success)
  ? () -> sendVerificationMail(s)
  : () -> logError(((Result.Failure) result).getMessage());
}
```
