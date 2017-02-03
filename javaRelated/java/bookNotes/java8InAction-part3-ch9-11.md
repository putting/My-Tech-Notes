## Chapter 9 Default Methods (& static methods)

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

### Usage Patterns
- Optional methods: methods that do nothin. eg Iterator.remove() could now be
  `default void remove() { throw new UnsupportedOperationException();}`
- Multiple inheritance of behavior: As interfaces can have default methods. A class can implement many interfcaes.
  - eg Template pattern. COmmon generic method as in abstract class.
- Minimal interfaces with orthogonal (perpendicular or different I think) functionalities??
- Composing Interfaces: eg class Monster implements Rotatable, Moveable, Resizable will inherit all the default methods.
  i.e. its being able to compose classes of mutiple disparate functionality. **MORE FLEXIBLE**

### Multiple Inheritance? Yes of Behaviour NOT state.
  


## Chapter 10 Optional

## Chapter 11 CompletableFuture
