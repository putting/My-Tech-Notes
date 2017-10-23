# Memory Model

Good overview detailing threads and their access to memory.
[link](http://tutorials.jenkov.com/java-concurrency/java-memory-model.html)

NB. This is different to how HEAP is architected for GC.

This is thread access to shared resources and visibility.

## Summary of memory used by Java objects
By default, Java objects are fast to access, but can easily consume a factor of 2-5x more space than the “raw” data inside their fields. This is due to several reasons:

Each distinct Java object has an **“object header”, which is about 16 bytes** and contains information such as a pointer to its class. For an object with very little data in it (say one Int field), this can be bigger than the data.
Java Strings have about 40 bytes of overhead over the raw string data (since they store it in an array of Chars and keep extra data such as the length), and store each character as two bytes due to String’s internal usage of UTF-16 encoding. 
**Thus a 10-character string can easily consume 60 bytes.**
Common collection classes, such as HashMap and LinkedList, use linked data structures, where there is a “wrapper” object for each entry (e.g. Map.Entry). This object not only has a header, but also pointers (typically 8 bytes each) to the next object in the list.
Collections of primitive types often store them as “boxed” objects such as java.lang.Integer.


