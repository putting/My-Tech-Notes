# ConcurrentHashMap 

see here: http://www.baeldung.com/java-concurrent-map for full expl
The explanation below needs updating from above link.
Especially structure is arrah of nodes.
Uses atomic operation compare and swap for updates.
Reads do not block and they reflect the latest completed update?
The table buckets are initialized lazily, upon the first insertion. Each bucket can be independently locked by locking the very first node in the bucket.
Ensures that when 2 threads attempt to write to same key or value, that 1 thread execs completely before the other. ie they are NOT cache interleaved.
looks like mal is divided into segments, then a hash is calculated inside segment. see her
https://dzone.com/articles/how-concurrenthashmap-works-internally-in-java
good diagram and explanations here
http://javabypatel.blogspot.com/2016/09/concurrenthashmap-interview-questions.html

## Summary
Now we know What is ConcurrentHashMap in Java and when to use ConcurrentHashMap, it’s time to know and revise some important points about CHM in Java.

1. ConcurrentHashMap allows concurrent read and thread-safe update operation.
2. During the update operation, ConcurrentHashMap only locks a portion of Map instead of whole Map.
3. The concurrent update is achieved by internally dividing Map into the small portion which is defined by concurrency level.
4. Choose concurrency level carefully as a significantly higher number can be a waste of time and space and the lower number may introduce thread contention in case writers over number concurrency level.
5. All operations of ConcurrentHashMap are thread-safe. mmm contradicted below for put, putAll.
6. Since ConcurrentHashMap implementation doesn't lock whole Map, there is chance of read overlapping with update operations like put() and remove(). In that case result returned by get() method will reflect most recently completed operation from there start.
7. Iterator returned by ConcurrentHashMap is weekly consistent, fail-safe and never throw ConcurrentModificationException. In Java.
8. ConcurrentHashMap **doesn't allow null as key or value**.
9. You can use ConcurrentHashMap in place of Hashtable but with caution as CHM doesn't lock whole Map.
10. During putAll() and clear() operations, the concurrent read may only reflect insertion or deletion of some entries.

## How ConcurrentHashMap is implemented in Java
ConcurrentHashMap is introduced as an alternative of Hashtable and provided all functions supported by Hashtable with an additional 
feature called "concurrency level", which allows ConcurrentHashMap to partition Map. 
ConcurrentHashMap allows multiple readers to read concurrently without any blocking. 
This is achieved by partitioning Map into different parts based on concurrency level and locking only a portion of Map during updates. 
Default concurrency level is 16, and accordingly Map is divided into 16 part and each part is governed with a different lock. 
This means, 16 thread can operate on Map simultaneously as long as they are operating on different parts of the Map. 
This makes ConcurrentHashMap high performance despite keeping thread-safety intact.  
Though, it comes with a caveat: Since update operations like **put(), remove(), putAll() or clear() are not synchronized**, 
concurrent retrieval may not reflect most recent change on Map.
**So use putIfAbsent() which is thread safe. i.e Two threads trying to put at the same time.**

In case of putAll() or clear(), which operates on whole Map, concurrent read may reflect insertion and removal of only some entries. Another important point to remember is iteration over CHM, Iterator returned by keySet of ConcurrentHashMap are weekly consistent and they only reflect state of ConcurrentHashMap and certain point and may not reflect any recent change. Iterator of ConcurrentHashMap's keySet area also fail-safe and doesn’t throw ConcurrentModificationExceptoin..

Default concurrency level is 16 and can be changed, by providing a number which make sense and work for you while creating ConcurrentHashMap. Since concurrency level is used for internal sizing and indicate number of concurrent update without contention, so, if you just have few writers or thread to update Map keeping it low is much better. ConcurrentHashMap also uses ReentrantLock to internally lock its segments.
