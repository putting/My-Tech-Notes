# Functional Programming Style

## Nurkiewicz. Egs from Download server and Executor Service
Lessons learnt from:
[link 1](http://www.nurkiewicz.com/2015/06/writing-download-server-part-ii-headers.html)
[link 2](also http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html)

Use of Optionals whenever method params can be null.
To avoid if then else use this pattern:
`.map(Method Call returning boolean).orElse(false)` //Can this be used for non-Boolean methods? I presume so, but when is OrElse called when not present

When calling a method from 2 other methods with slightly different subsequent calls. **Pass the method call as a Function**
Eg Would be called using this method ref: `this::serveDownload
                private ResponseEntity<Resource> methodName(Function<FilePointer, ResponseEntity<Resource>> notCachedResponse) {
                                if (cached(requestEtagOpt, ifModifiedSinceOpt))  notModified(filePointer);
                                return notCachedResponse.apply(filePointer);  **//See how this then calls 1 or more methods defined by the caller**
                }`

Also describes how servlet filters and ETag caching in browsers for resources work to ALLOW STREAMING of body.

Some v interesting parts about downloading partial bytes of the file at a time using Range.

Generally make properties that are not final Optional. Especially in public apis. Also this is very useful when object chained together as you can do:
`Person.flatMap(Person::getAddress).flatMap(Address::getValidFrom).filter(x -> x.before(now())).isPresent();`

NB. Throttling download speed. Linux pv command can be used to monitor the download of a file:
curl localhost:8080/download/4a8883b6-ead6-4b9e-8979-85f9846cab4b | pv > /dev/null
