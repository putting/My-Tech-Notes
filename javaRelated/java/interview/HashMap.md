# Interview Questions on Maps

Hashtable, ConcurrentHashMap, LinkedHashMap etc but HashMap is your general purpose map.
LinkedHashMap = Preserves order of mapping.
TreeMap = Sorted Impl.
ConcurrentHashMap=Thread safe.

## Questions

### How does put() method of HashMap work in Java?
The put() method of HashMap works in the principle of hashing. It is responsible for storing an object into backend array. 
The hashcode() method is used in conjunction with a hash function to find the correct location for the object into the bucket. 
If a collision occurs then the entry object which contains both key and value is added to a linked list and that linked list is stored into the bucket location.

### What is the requirement for an object to be used as key or value in HashMap?
The key or value object must implement equals() and hashcode() method. 
The **hash code is used when you insert the key object** into the map while **equals are used when you try to retrieve a value** from the map.

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

Read more: http://www.java67.com/2013/06/how-get-method-of-hashmap-or-hashtable-works-internally.html#ixzz58tTXSrgt

### Which data structure HashMap represents?
The HashMap is an implementation of hash table data structure which is idle for mapping one value to other 
e.g. id to name as you can search for value in O(1) time if you have the key.

### Which data structure is used to implement HashMap in Java?
Even though HashMap represents a hash table, it is internally implemented by using an array and linked list data structure in JDK. 
The array is used as bucket while a linked list is used to store all mappings which land in the same bucket. 
From Java 8 onwards, the linked list is dynamically replaced by binary search tree, once a number of elements in the linked list cross a certain threshold to improve performance.

### Can you store a duplicate key in HashMap?
No, you cannot insert duplicate keys in HashMap, it doesn't allow duplicate keys. If you try to insert an existing key with new or same value then it will override the old value but size of HashMap will not change 
i.e. it will remain same. This is one of the reason when you get all keys from the HashMap by calling keySet() it returns a Set, not a Collection because Set doesn't allow duplicates.

### Can you store the duplicate value in Java HashMap?
Yes, you can put duplicate values in HashMap of Java. It allows duplicate values, that's why when you retrieve all values from the Hashmap by calling values() method it returns a Collection and not Set. 
Worth noting is that it doesn't return List because HashMap doesn't provide any ordering guarantee for key or value
