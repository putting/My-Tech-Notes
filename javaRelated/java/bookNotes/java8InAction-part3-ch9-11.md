## Chapter 9 Interfaces: Default Methods (& static methods)

The main users of default methods are library designers. 
- *Q. So are there any other patterns for use?*
  - Can help evolve Public api's
Can add new default methods without breaking existing classes implementing.
- Removing utility classes, put static methods on interface.
  A common pattern in Java is to define both an interface and a utility companion class defining many static methods for working with instances of the interface. eg, Collections is a companion class to deal with Collection objects.
- Binary Compatible
Adding a new method to an interface is binary compatible; this means existing class file implementations will still run without the
implementation of the new method, if there’s no attempt to recompile them.

### Abstract class vs Interface
- a) a class can extend only from one abstract class, but a class can implement multiple interfaces.
- b) an abstract class can enforce a **common state** through instance variables (fields). An interface can’t have instance variables.

### Usage Patterns: Optional methods & Multiple inheritance
- Optional methods: methods that do nothin. eg Iterator.remove() could now be
  `default void remove() { throw new UnsupportedOperationException();}`
- Multiple inheritance of behavior: As interfaces can have default methods. A class can implement many interfcaes.
  - eg Template pattern. COmmon generic method as in abstract class.
- Minimal interfaces with orthogonal (perpendicular or different I think) functionalities??
- Composing Interfaces: eg class Monster implements Rotatable, Moveable, Resizable will inherit all the default methods.
  i.e. its being able to compose classes of mutiple disparate functionality. **MORE FLEXIBLE**

### Multiple Inheritance? Yes of Behaviour NOT state.
  Keep interfaces minimal - You can then use them for composing with many classes. If you over populate an interface, fewe classes will want to implement all the methods.

## Chapter 10 Optional
The intention of the Optional class is not to replace every single null reference. Instead, its purpose is to help you design more-comprehensible APIs so that by just reading the signature of a method, you can tell whether to expect an optional value.

### Ensure all NON final vars return Optional?
  May well be worthwile doing, for embedded objects definitely, not so much primitives. To deal with the chaining issue
  - `person.getCar().getInsurance().getName();` can throw NPE if person does not have a car.
  - Calling method either ends up nesting `if null` or has `if return` statements.
  - null doesn't convey meaning and doesn't have any type info. its wrong.

### Using Optional in Domain Entities
  - The domain is clear. A person might now own a car: `class Person private Optional<Car> car; Optional<Car> getCar();` 

### Creating optionals
  - An Empty: `Optional<Car> optCar = Optional.empty();`
  - From an non-null var: `Optional<Car> optCar = Optional.of(car);`
  - From a var that may be null: `Optional.ofNullable(car);` If car was null ht eoptional would be empty.

### Extracting values from Optional (NB. Not Serializable)
  get causes an Ex if the optional is empty so look into other ways
  - `Optional<Insurance> optInsurance = Optional.ofNullable(insurance); Optional<String> name = optInsurance.map(Insurance::getName);'
  If the Optional contains a value, then the function passed as argument to map transforms that value. If the Optional is empty, then nothing happens.
  - When chaining optionals use flatMap (as otherwise the method ref wont work as actually the obj is an optional)
  **Chaining Solution:** `person.getMap(Person::getCar).flatMap(Car::GetInsurance).map(Insurance::getName).orElse("Unkown");
  If any of the method refs return Optional.empty, it will be passed to the end where orElse will be invoked.
  - NB. Optionals can be passed as params too.
  - Optionals on Domain are NOT serializable! Intended to be return Optional not `Optional<Car> car =..` as a variable.
  so the solution is don;t make var opt just return: `private Car car; public Optional<Car> getCarAsOptional() {return Optional.ofNullable(car);}`
  - i) get() Simpletest BUT least safe throws NoSuchElementException. Bad idea unless you know present.
  - ii) orElse(T other) : simple default
  - iii) orElseGet(Supplier<? extends T> other) is the lazy counterpart of the orElse method, because the supplier is invoked only if the optional contains no value. 
  - iv) orElseThrow(Supplier<? extends X> exceptionSupplier)
  - v) ifPresent(Consumer<? super T> consumer) lets you execute the action given as argument if a value is present; otherwise no action is taken
  - See how you can replace this (similar to old style)
``` java
public Optional<Insurance> nullSafeFindCheapestInsurance(
Optional<Person> person, Optional<Car> car) {
if (person.isPresent() && car.isPresent()) {
return Optional.of(findCheapestInsurance(person.get(), car.get()));
} else {
return Optional.empty();
```
  with & this demos some v important ideas about chaining
```java
public Optional<Insurance> nullSafeFindCheapestInsurance(Optional<Person> person, Optional<Car> car) {
return person.flatMap(p -> car.map(c -> findCheapestInsurance(p, c)))
```
Invoke a flatMap on the first optional, if  empty, the lambda expression passed to it won’t be executed at all and this invocation will just return an empty optional. Conversely, if the person is present, it uses it as the input of a Function returning an
Optional<Insurance> as required by the flatMap method. The body of this function invokes a map on the second optional, so if it doesn’t contain any car, the Fn will return an empty optional and so will the whole nullSafeFindCheapestInsurance method. Finally, if both the person and the car are present, the lambda expression passed as argument to the map method can safely invoke the original findCheapestInsurance method with them.
- Filtering based on predicate (replace if test then):
  `Optional<Insurance> optInsurance = ...; 
    optInsurance.filter(insurance -> "CambridgeInsurance".equals(insurance.getName()))
    .ifPresent(x -> System.out.println("ok"));`
    
###  Replacing 3rd party libs that return null (the following can be used as utility Optional classes)
  - Wrap the return from a method with map get: `Optional<Object> value = Optional.ofNullable(map.get("key"));`
  - Not sure about wrapping Integer.parseint(str) NumberFormatEx into Optional.empty (see p305)
  - **Don't use primitive optionals as dont provide flatMap & filter methods.**
  - eg reading from a properties file:
``` java
return Optional.ofNullable(props.getProperty(name))
.flatMap(OptionalUtility::stringToInt)
.filter(i -> i > 0)
.orElse(0);
```
  
  

## Chapter 11 CompletableFuture
