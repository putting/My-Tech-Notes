#Threading

##CompletableFuture
[link](http://www.nurkiewicz.com/2013/05/java-8-definitive-guide-to.html)
[Really good expln of “Writing async code with CompletableFuture”](http://www.deadcoderising.com/java8-writing-asynchronous-code-with-completablefuture/)

Includes some good egs of more advanced options and scenarios

Like when using thenApply for callbacks (runs in the same thread as orig future)
Whereas thenApplyAsync runs in new thread

Chaining egs 
`CompletableFuture<Double> f3 = 
    f1.thenApply(Integer::parseInt).thenApply(r -> r * r * Math.PI);`

But what's most important, these transformations are **neither executed immediately nor blocking**. 
They are simply remembered and when original f1 completes they are executed for you. 
If some of the transformations are time-consuming, you can supply your own Executor to run them asynchronously.

And exception handling..
`CompletableFuture<Integer> safe = future.handle((ok, ex) -> {
    if (ok != null) {
        return Integer.parseInt(ok);
    } else {
        log.warn("Problem", ex);
        return -1;
    }
});`

thenCompose() is an essential method that allows building robust, asynchronous pipelines, without blocking or waiting for intermediate steps

