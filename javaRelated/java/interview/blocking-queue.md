# BlockingQueue using LinkedList and Thread Comms with wait() & notify()

[http://tutorials.jenkov.com/java-concurrency/blocking-queues.html](http://tutorials.jenkov.com/java-concurrency/blocking-queues.html)

So this approach uses a LinkedList to create similar object to the java LinkedBlockingQueue.
NB. These are the java Blocking Queues: 

  ArrayBlockingQueue, DelayQueue, LinkedBlockingDeque, LinkedBlockingQueue, LinkedTransferQueue, PriorityBlockingQueue, SynchronousQueue

``` java
public class BlockingQueue {

  private List queue = new LinkedList();
  private int  limit = 10;

  public BlockingQueue(int limit){
    this.limit = limit;
  }

  public synchronized void enqueue(Object item) throws InterruptedException  {
    while(this.queue.size() == this.limit) {
      wait();
    }
    if(this.queue.size() == 0) {
      notifyAll();
    }
    this.queue.add(item);
  }

  public synchronized Object dequeue() throws InterruptedException{
    while(this.queue.size() == 0){
      wait();
    }
    if(this.queue.size() == this.limit){
      notifyAll();
    }
    return this.queue.remove(0);
  }
}
```
