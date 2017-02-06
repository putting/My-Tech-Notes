# Interfaces

## Default Methods
### 1) As behaviour has been added by default methods in interfaces BUT NOT STATE. Single inheritance of state via classes remains.
Introducing default methods extends the role of interfaces to provide behavior, so multiple inheritance of behavior
would now have to be allowed. The Java 8 designers were able to devise rules that make the problems with behavior inheritance
manageable; the classic problems of **multiple inheritance are associated with inheritance of state, which Java does not and never will support.**

### 2) Rather than use the old static convenience classes, new methods can be added which are more efficient:
Default methods provide opportunities to relocate existing functionality in more appropriate places. 
eg. to sort a List in reverse natural order before Java 8, you had to call two static Collections methods:
`Collections.sort(integerList,Collections.reverseOrder());`
Now, however, the default method List.sort (together with a static interface method Comparator.reverseOrder) allows this code to be
rewritten in a more readable way:
`integerList.sort(Comparator.reverseOrder());`
In fact, List.sort is more than a convenience; it allows the List class to override sort with an efficient implementation, using
knowledge of its internal representation that is unavailable to the static method Collections.sort.

### 3) Why use Abstract classes now?
The abstract classes should provide the getters/setters to the instance fields. 
Interfaces and new default methods can use the underlying methods to get access to the instance data – transforming in some way. 
The key here is that interfaces don’t have access directly to instance variables. Although can refer to **this**. 

### 4) Principles:
a) Default methods can never override instance methods, whether inherited or not.
b) Instance methods are chosen in preference to default methods. “classes win over interfaces.” Important to stop new default methods changing existing behaviour.
c) If more than one competing default method is inherited by a class, the non-overridden default method* is selected. Ie. a default method not overridden by any other
that is also inherited by the class. That is the ‘lowest’ interface method – so not overridden by other interfaces or classes. 
Will fail if there is NO overridden method with “inherits unrelated defaults for…” but you can be explicit in concrete class `Foo.Super.hello()` when calling method.

TODO: Some egs required.....
