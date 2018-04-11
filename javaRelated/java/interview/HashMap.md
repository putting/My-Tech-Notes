# Interview Questions on Maps

Hashtable, ConcurrentHashMap, LinkedHashMap etc but HashMap is your general purpose map.
LinkedHashMap = Preserves order of mapping.
TreeMap = Sorted Impl.
ConcurrentHashMap=Thread safe.

[full description of put/get here](http://www.java67.com/2013/06/how-get-method-of-hashmap-or-hashtable-works-internally.html)

## Not Thread-Safe
- The Getting of a value, so it can be then set is NOT atomic. 
  Thread a does get(key) and returns null. Then goes to sleep. Thread B does get null and then sets value. Thread A awakes and then overwrites the preious value. eg. Using Incrementer based on current value.
- ConcurrentHashMap provided better throughput, BUT also methods which are atomic for getting and setting.

## Questions

### How does put() method of HashMap work in Java?
The put() method of HashMap works in the principle of hashing. It is responsible for storing an object into backend array. 
The hashcode() method is used in conjunction with a hash function to find the correct location for the object into the bucket. 
If a collision occurs then the entry object which contains both key and value is added to a linked list and that linked list is stored into the bucket location.

### What is the requirement for an object to be used as key in HashMap?
The key or value object must implement equals() and hashcode() method. 
The **hash code is used when you insert the key object** into the map while **equals are used when you try to retrieve a value** from the map.

### What happens if hascode and Equals NOT overriden (hashcode contract)
If hashCode is used as a shortcut to determine equality, then there is really only one thing we should care about: 
**Equal objects should have the same hash code.**
This is also why, if we override equals, we must create a matching hashCode implementation! Otherwise things that are equal according to our implementation would likely not have the same hash code because they use Object‘s implementation.

### What will happen if you try to store a key which is already present in HashMap?
If you store an existing key in the HashMap then it will override the old value with the new value and put() will return the old value. 
There will not be any exception or error.

### Can you store a null key in Java HashMap? 
Yes, HashMap allows one null key which is stored at the first location of bucket array e.g. bucket[0] = value. 
The HashMap doesn't call hashCode() on null key because it will throw NullPointerException, hence when a user call get() method with null then the value of the first index is returned.

### Can you store a null value inside HashMap in Java? 
Yes, HashMap also allows null value, you can store as many null values as you want as shown in the hashmap example post in this blog.

### How does HashMap handle collisions (on hashcode) in Java? 
The java.util.HashMap uses chaining to handle collisions, which means new entries, an object which contains both key and values, are stored in a linked list along with existing value and then that linked list is stored in the bucket location. 
**In the worst case**, where all key has the same hashcode, your hash table will be turned into a linked list and searching a value will take O(n) time as opposed to O(1) time.
**This has also changed from Java 8, where after a threshold is crossed then a binary tree is used instead of linked list to lift the worst case performance from O(n) to O(logN).**

#### Performance Diff between LinkedList and Balanced Tree
A linked list has each element point to the next one(basically an array without a size limit), while a binary tree (**balanced**) has each element having two nodes, one pointing to a number less than its value and one pointing to a number with a greater value. This, in theory, cuts search times down by a lot, because if an element’s the last element in a list with n elements, it would take n operations to find it, whereas in a balanced(meaning that no branch of the binary tree is too full or too empty) binary tree with n items in it, it would have a max search time of log(n)(Base 2).

### Which data structure does a HashMap represent?
The HashMap is an implementation of **hash table data structure** which is idle for mapping one value to other 
e.g. id to name as you can search for value in O(1) time if you have the key.

### Which data structure is used to implement HashMap in Java?
Even though HashMap represents a hash table, it is internally implemented by **using an array and linked list data** structure in JDK. 
The array is used as bucket while a linked list is used to store all mappings which land in the same bucket. 
From Java 8 onwards, the linked list is dynamically replaced by binary search tree, once a number of elements in the linked list cross a certain threshold to improve performance.

### What will happen if two different keys of HashMap return same hashcode()?
If two keys of HashMap return same hash code then they will end up in the same bucket, hence collision will occur. They will be stored in a linked list together.

### Can you store a duplicate key in HashMap?
No, you cannot insert duplicate keys in HashMap, it doesn't allow duplicate keys. If you try to insert an existing key with new or same value then it will override the old value but size of HashMap will not change 
i.e. it will remain same. This is one of the reason when you get all keys from the HashMap by calling keySet() it returns a Set, not a Collection because Set doesn't allow duplicates.

### Can you store the duplicate value in Java HashMap?
Yes, you can put duplicate values in HashMap of Java. It allows duplicate values, that's why when you retrieve all values from the Hashmap by calling values() method it returns a Collection and not Set. 
Worth noting is that it doesn't return List because HashMap doesn't provide any ordering guarantee for key or value

### Is HashMap thread-safe in Java?
No, HashMap is not thread-safe in Java. You should not share an HashMap with multiple threads if one or more thread is modifying the HashMap e.g. inserting or removing a map. Though, you can easily share a read-only HashMap.

### What will happen if you use HashMap in a multithreaded Java application?
If you use HashMap in a multithreaded environment in such a way that multiple threads structurally modify the map e.g. add, remove or modify mapping then the internal data structure of HashMap may get corrupt i.e. some links may go missing, some may point to incorrect entries and the map itself may become completely useless. Hence, it is advised not to use HashMap in the concurrent application, instead, you should use a thread-safe map e.g. ConcurrentHashMap or Hashtable.

### What are different ways to iterate over HashMap in Java?
Here are some of the ways to iterate over HashMap in Java:
by using keySet and iterator, by using entrySet and iterator, by using entrySet and enhanced for loop, by using keySet and get() method

### How do you remove a mapping while iterating over HashMap in Java?
Even though HashMap provides remove() method to remove a key and a key/value pair, you cannot use them to remove a mapping while traversing an HashMap, instead, you need to use the Iterator's remove method to remove a mapping as shown in the following example:

```java
Iterator itr = map.entrySet().iterator();

while(itr.hasNext()){
  Map.Entry current = itr.next();

  if(current.getKey().equals("matching"){
     itr.remove(); // this will remove the current entry.
  }
}
```
You can see that we have used Iterator.remove() method to remove the current entry while traversing the map.

### In which order mappings are stored in HashMap?
Random order because HashMap doesn't provide any ordering guarantee for keys, values, or entries. When you iterate over an HashMap, you may get the different order every time you iterate over it.

### Can you sort HashMap in Java?
No, you cannot sort a HashMap because unlike List it is not an ordered collection. Albeit, you can sort contents of HashMap by keys, values or by entries by sorting and then storing the result into an ordered map e.g. LinkedHashMap or a sorted map e.g. TreeMap.

### What is load factor in HashMap?
A load factor is a number which controls the resizing of HashMap when a number of elements in the HashMap cross the load factor e.g. if the load factor is 0.75 and when becoming more than 75% full then resizing trigger which involves array copy.

### How does resizing happens in HashMap? 
The resizing happens when map becomes full or when the size of map crosses the load factor. For example, if the load factor is 0.75 and when become more than 75% full then resizing trigger which involves array copy. First, the size of the bucket is doubled and then old entries are copied into a new bucket.

### How many entries you can store in HashMap? What is the maximum limit?
There is no maximum limit for HashMap, you can store as many entries as you want because when you run out of the bucket, entries will be added to a linked list which can support an infinite number of entries, of course until you exhaust all the memory you have.

Btw, the size() method of HashMap return an int, which has a limit, once a number of entries cross the limit, size() will overflow and if your program relies on that then it will break. This issue has been addressed in JDK 8 by introducing a new method called mappingCount() which returns a long value. So, you should use mappingCount() for large maps. See Java SE 8 for Really Impatient to learn more about new methods introduced in existing interfaces in JDK 8.

### What is the difference between capacity and size of HashMap in Java?
The capacity denotes how many entries HashMap can store and size denotes how many mappings or key/value pair is currently present.

## Good Summary
A hash table's strength is its fast lookup and insertion speed. To get that speed, one must forsake any semblance of order in the table: i.e. entries are all jumbled up. A list is acceptable to use as a table entry because while traversal is O(n), the lists tend to be short assuming the hash table is sufficiently large and the objects stored in the table are hashed using a good quality hashing algorithm.

A binary search tree (BST) has fast insertion and lookup at O(log2 n). It also imposes a restriction on the elements it stores: there must be some way to order the elements. Given two elements A and B stored in the tree, it must be possible to determine if A comes before B or if they have equivalent order.

A hash table imposes no such restriction: elements in a hash table must have two properties. First, there must be a way to determine if they are equivalent; second, there must be a way to calculate a deterministic hash code. Order is not a requirement.

If your hash table elements do have an order, then you can use a BST as a hash table entry to hold objects with the same hash code (collisions). However, due to a BST having O(log2 n) lookup and insertion, that means the worst case for the whole structure (hash table plus BST) is technically better than using a list as a table entry. Depending on the BST implementation it will require more storage than a list, but likely not much more.

Please note that normally the overhead and behavior of a BST brings nothing to the table in real world situations as hash table buckets, which is why the theoretical poor performance of a list is acceptable. In other words, the hash table compensates for the list's weakness by placing fewer items in each list (bucket). However: the problem specifically stated that the hash table cannot increase in size, and collisions are more frequent than is typical in a hash table.
