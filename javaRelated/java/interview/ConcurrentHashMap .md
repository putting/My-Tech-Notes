# ConcurrentHashMap 
Very good artcile here: http://javabypatel.blogspot.com/2016/09/concurrenthashmap-interview-questions.html


## Summary
ConcurrentHashMap (CHM) is the same as HashMap, but includes an additional array of *Segments*.
Locking is applied at the segment level. So two writes to the same segment will block. Guarantees that 1 will NOT be interleaved with other. 

Q. Why is HashMap not thread-safe then? Infinite loop problem, where rehashing while being read by another thread. Interleaving also?
Offers O(1) time complexity for both get and put operation.

### Structure
An index of Segments. Each of which is a pointer to a single **HashTable NOT HashMap**. Some diagrams in above are incorrect.
- Segment array -> based on the concurrency level
- HashEntry array (result of bucketing) -> using hashcodes
- LinkedList of HashEntrys

### Sizing
new ConcurrentHashMap(initialCapacity, loadFactor, concurencyLevel)
SegmentSize(array default 16) = nearest 2*n >= currencyLevel. So default cl=16, (2*3=8 too low)2*4=16.
HashBucketSize(array) = 

## Performance
- Locking is per array of nodes as table buckets (prior java 8 Segments).
- Increase no of nodes to improve concurrency. Trade-off if un-used segments, then wasted locks/space of additional HashMap.
- Reads are non-blocking
- O(1) with help of
  - Good hashcode method which evenly distributes across all buckets
  - Each put checks Threashold limit of 0.75, if so then rehashing.
  - Rehashing occurs also when too many key collisions, which increases entries in a bucket.

## Thread-Safety
- Two threads writing to same segment/data will block T2 one UNTIL T1 is completed. ie **no interleaving**
  - HashMap is NOT synchronised so interleaving of threads occurs.
- Does NOT allow null key or value.
- Reads are non-blocking. If an update and read occur in same segment, the returned value will reflect the most recently
completed update.
- HasMap has infinite looping issue: http://javabypatel.blogspot.com/2016/01/infinite-loop-in-hashmap.html

## Main Points
- Put CHM. Determine node index, then as with HashMap:
- PUT HashMap. 
  a) Get hashcode from key
  b) Evaluate hashcode to determine bucket.
  c) Check key against linkedList keys. If exists, update, else insert at end
- Re-hashing moved entries into different buckets to limit the length of linkedLists

## Issues
- Design of hascode and equals. What is the contract?
  - Two objects can have same hashcode, BUT equals must uniquely identify.
  - Objects that are equal return the same hashcode.
  - Always override hascode and equals. 

## Hash code
- Worst design is single hashcode for all keys. This is a linkedList
- All different hascodes - Each HashMap is checked to see how full (eg 75%) OR too many collisions.
  - Entries can be moved too different buckets to distribute more evenly over same No of Buckets (default 16)

### Further Reading
1) see here: http://www.baeldung.com/java-concurrent-map for full expl
The explanation below needs updating from above link.
Especially structure is arrah of nodes.
Uses atomic operation compare and swap for updates.
Reads do not block and they reflect the latest completed update?
The table buckets are initialized lazily, upon the first insertion. Each bucket can be independently locked by locking the very first node in the bucket.
Ensures that when 2 threads attempt to write to same key or value, that 1 thread execs completely before the other. ie they are NOT cache interleaved.
looks like mal is divided into segments, then a hash is calculated inside segment.

2) good diagram and explanations here
https://dzone.com/articles/how-concurrenthashmap-works-internally-in-java

https://www.geeksforgeeks.org/concurrenthashmap-in-java/

