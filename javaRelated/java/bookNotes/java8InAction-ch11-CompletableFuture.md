# Chapter 11 CompletableFuture

### Concurrenct vs Paralllelism
- Parallelism: Is use of multi-core/ machines to split tasks.
- Concurrency: More about non-blocking. eg calls to slow services (eg remote), may block calling thread

### Futures
- Good , but the calling thread will still need to check and get value (which is blocking (with timeout)).

### CompletableFuture methods
  - Converting a synchronous api method call to async
  - Can make a method async and return CF as |Future and then do get on it.
  - **NB. Excedtions get lost in async threads** This can block the calling thread get() unless a Timout specified.
  But better to do cfuturePrice.completeExceptionally(ex) in async method.
  
### Factory methods
  - Supply Async: `CompletableFuture.supplyAsync(() -> calculatePrice(product));` and is much more succinct than new Thread egs.
  
### Chapter assumes api of Shop only provideds sync blocking methods.
  - Also discusses that typically is the case with HTTP api's provided by a service.
  **NB** Design non-blockinh RESTful WS? or wrap returns as discussed here?
  
### Improving calls to  slow method called for 4 diff shops:  `public List<String> findPrices(String product);`
  - Stream in parallel: shops.parallelStream().map(shop -> String.format("%s price is %.2f",shop.getName(), shop.getPrice(product))).collect(toList());
  - Async (NB warning of pipelining has an effect on async submission): 
``` java
List<CompletableFuture<String>> priceFutures =
  shops.stream()
    .map(shop -> CompletableFuture.supplyAsync(
      () -> String.format("%s price is %.2f", shop.getName(), shop.getPrice(product))))
    .collect(toList());
```
  Note that this only returns all futures, as findPrices returns List<String> you still need to wait till all finish, then
  apply this to above variable:
  `priceFutures.map(CompletableFuture::jon).collect(toList());` to join all results together
  **Understand that the above is in 2 parts - as if done as single pipeline would have been async due to lazy intermediate tasks** see p323 more detail
  
### Custom Executor
In this case if the number of async service calls you are making can be matched to exact amount of cores, then useful optimisation.
Use Daemon threads so as not to prevent jvm termination.

### Pipelining: thenCombine, thenApply, thenCompose
  - Think of streaming collections, which in turn might make service calls:
  `shops.stream.map(shop -> shop.getPrice(prod).map(Discount::applyDiscount.collect(toList());`     
  - In CF terms above is more complicated as using suppliers for CF async
``` java
shops.stream.  
  map(shop -> CompletableFuture.supplyAsync(() -> shop.getPrice(prod)))  //should be a thenApply method non async
  .map(future -> future.thenCompose(quote -> CompletableFuture.supplyAsync(() -> Discount::applyDiscount(quote)) //NB thenCompose
  .collect(toList());
  return pricesFuture.stream().map(CompletableFuture::join).collect(toList()); //in 2 parts for async
```
### Reacting to a CompletableFuture completion (callbacks)
- use thenAccept: `someCF.map(f -> f.thenAccept(System.out::println));`
- where findPricesStream method returns Stream<CompletableFuture<String>>
``` java
  CompletableFuture[] futures = findPricesStream("myPhone")
  .map(f -> f.thenAccept(System.out::println))
  .toArray(size -> new CompletableFuture[size]);
  CompletableFuture.allOf(futures).join();
```

  
