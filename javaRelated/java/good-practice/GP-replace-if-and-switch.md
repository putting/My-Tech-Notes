# Replacing Nested if and switch statements

Start with a problem:
``` java
switch(operation)
case addition  : return add(int a, String b, String c);
case multiply  : return multiply(int a, int b);
case substract : return substract(int a, int b);
```
We have a few options to replace this:
  - Polymorphism. eg `actions.add(addition, new addOperation()); ...`
  - Enum: `ADDITION {public void performOperation(int a, int b) { ...` 
  - Lambdas: If you've a method which has the same signature of your interface you can also pass it to your operation repository like:
``` java
Map<String, IntBinaryOperator> operations = new HashMap<>();
operations.put("add", Integer::sum);
operations.put("subtract", (a, b) -> a - b);
operations.put("multiply", (a, b) -> a * b);
//...
System.out.println(operations.get("multiply").applyAsInt(10, 20));
```
