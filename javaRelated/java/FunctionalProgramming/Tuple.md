# Tuple defined as helper

## Reason for using Tuples
You’ll often need such a class to hold two (or more) values, because functional programming replaces side effects with returning a representation of these effects. 
This avoid creating composite objects just to return from fns.

- Ensures objects can't be null
- Tuple3,4 etc.. can also be created.

```java
public class Tuple<T, U> {
  public final T _1;
  public final U _2;
  public Tuple(T t, U u) {
    this._1 = Objects.requireNonNull(t);
    this._2 = Objects.requireNonNull(u);
}

@Override
public boolean equals(Object o) {
  if (!(o instanceof Tuple)) return false;
  else {
    Tuple3 that = (Tuple) o;
    return _1.equals(that._1) && _2.equals(that._2);
  }
}
@Override
public int hashCode() {
  final int prime = 31;
  int result = 1;
  result = prime * result + _1.hashCode();
  result = prime * result + _2.hashCode();
  return result;
  }
}  

```
