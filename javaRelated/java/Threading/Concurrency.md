# Concurrency

Good set of articles on scala and new java 8 concurrency objects for Event (non-blocking) as opposed to future (blocking)

Its interesting how the orchestrating thread will wait for a future (can poll and then see if thread finished) = Blocking.

Event based is more like a callback where the worker thread notifies when its finished.

[Java8](http://www.ibm.com/developerworks/java/library/j-jvmc3/index.html)
[Java 8 concurrency Basics](http://www.ibm.com/developerworks/java/library/j-jvmc2/index.html)
