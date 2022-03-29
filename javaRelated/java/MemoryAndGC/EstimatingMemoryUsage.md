# Estimating Memory Uage

## Java Object Size: Estimating, Measuring, and Verifying via Profiling
https://dzone.com/articles/java-object-size-estimation-measuring-verifying
Discusses in depth:
- Java Primitive: 1 byte (byte(nb int -128 to 128), boolean), 2 byte (char, short), 4 byte (int, float), 8 byted (long, double)
- Memory Word: 32 bit = 8 bytes, 64 bit = 16bytes
- Java Object: Header: Mark word (locks, gc info, hashcode) 8 bytes, klass pointer (block pointer, array length) 4 bytes
  - So wrappers size in general = object header object + internal primitive field size  + memory gap (8/16byte round up)
- Arrays
- String

### Java Class
Now we know how to calculate Java Object, Java Primitive, and Java Primitive Wrapper and Arrays. Any class in Java is nothing but an object with a mix of all mentioned components:
-    header (8 or 12 bytes for 32/64 bit os).
-    primitive (type bytes depending on the primitive type).
-    object/class/array (4 bytes reference size).


## How to Estimate Object Memory Allocation in Java
https://dzone.com/articles/how-to-estimate-object-memory-allocation-in-java

eg Using VisualVM to see heap sized used by objects. ie How they are allocated
