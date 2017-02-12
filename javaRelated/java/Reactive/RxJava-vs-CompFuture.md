# Why use RxJava over CompletableFuture

NB. CF is is newer so replaces some of what RxJava does.
**BUT** there is a difference in emphasis as Reactive is more about streams and **pushing** events.

BUT basically as a comment says CompletableFuture composing is the best non-blocking way to implement this.

## CompletableFuture vs RxJava
Really it’s the PUSH of RxJava rather than the ‘PULL’ from collections of Iterators (including Streams etc..) that is diff.
Here the subscribe params are onNext, onError, OnCompleted
``` java
final Observable<String> observable = Observable.from(list);
        observable.subscribe(System.out::println, System.out::println, () -> System.out.println("We are done!"));
```        
Whereas CF would allow you to perform a call Back when **ENTIRE** async call finished.
` CompletableFuture.supplyAsync(this::sendMsg).thenApply(System.out.::prinltn)`
-	Simple eg where it is the composing of multiple thread is main purpose.
[https://dzone.com/articles/using-java-8-completablefuture-and-rx-java-observa](https://dzone.com/articles/using-java-8-completablefuture-and-rx-java-observa)
The RxJava is simpler the Create Observable emits immediately & they can be merged simply. But no conclusion.

Some discussion on how RxJava can be tough to learn and debug and is really about streams.
This has some good egs but a lot on clojure!:
[https://github.com/ReactiveX/RxJava/wiki/How-To-Use-RxJava]( https://github.com/ReactiveX/RxJava/wiki/How-To-Use-RxJava)
Observables created using Just() & from() use an existing object like a list
So These converted Observables will synchronously invoke the onNext( ) method of any subscriber that subscribes to them, for each item to be emitted by the Observable, and will then invoke the subscriber’s onCompleted( ) method.
**Q**. I think List<Integer> emits single  Integer’s from `Observable<Integer> o = Observable.from(list)`
Create() starts from scratch.

[link Discusses converting between CF’s and Observable & vice versa]( https://blog.krecan.net/2015/04/28/converting-rxjava-observables-to-java-8-completable-future-and-back/)

### Demonstrate how Future is still blocking
Discusses how to work around.
[https://gist.github.com/benjchristensen/4671081](https://gist.github.com/benjchristensen/4671081)

Good overview & in-depth discussion. It goes a little too deep into javascript (using marble diagrams)
https://gist.github.com/staltz/868e7e9bc2a7b8c1f754

this article discusses Reactive web front ends & responding to user clicks.
However there is a link to server side Netflix reactive programming
**Contains some v interesting stuff** Uses Groovy egs BUT v understandable
[http://techblog.netflix.com/2013/02/rxjava-netflix-api.html]( http://techblog.netflix.com/2013/02/rxjava-netflix-api.html)
Discusses non-blocking approach to **services**: Which return Observable<T>, either immediately from cache OR by submitting new Threaded remote svc call.
So wither do: `observer.onNext(valueFromMemory);` which returns data immediately OR 
` //Get the data:  T value = getValueFromRemoteService(id);   // then callback with value observer.onNext(value);` Role of subscription here?
Compose using `Observable.zip()`

**Creating an Observable is creating a STREAM** This Stream is the core principle. Eg for a single item stream. NB author says Observable silly name hence stream!
` Observable<String> requestStream = Observable.just("https://api.github.com/users");`
You can then subscribe to this Observable Stream.
Then subscribe a Callback to do something when value emitted.. **This actually executes request**

` Rx.Observable.create()` creates your own custom stream, by explicity informing each subscriber.
