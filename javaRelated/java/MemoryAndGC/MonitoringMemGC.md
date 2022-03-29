# Monitoring Memory and GC

## Lightrun Metrics
Allows debugging of production code
https://lightrun.com/

eg Debugging hashcode and equals
https://dzone.com/articles/debugging-java-equals-and-hashcode-performance-in

## GCeasy tool for analyzing gc logs
https://dzone.com/articles/garbage-collection-tuning-success-story-reducing-y

**‘Allocation failure’** GC events are triggered when there is not sufficient memory to create new objects in the young generation.
Counter-intuitevely they reduced the NewGen size to reduce GC pauses:

`Since the young generation’s size is very large, a lot of objects in this generation have to be scanned and unreferenced objects 
from this region have to be evicted. Thus, the larger the young generation’s size, the larger the pause time`
