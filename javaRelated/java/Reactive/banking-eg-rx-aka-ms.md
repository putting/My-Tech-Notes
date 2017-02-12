
# RxJava (and Akka) MicroServices & Reactive Bank Examples

**A MUST READ**

Both are Account & Transaction egs:
Reactive eg: [https://github.com/mrudul03/reactive-examples](https://github.com/mrudul03/reactive-examples)
MicroServices eg: More important see the full app for a complete sys: https://github.com/mrudul03/microservices-demo

Microservices require MORE inter-service hops & sometimes uses aggregator (composite) svcs. See discussion: also CompletableFuture 
https://github.com/mrudul03/reactive-examples

## @Suspended AsyncResponse 
    - Interesting async handling both on server & client side implemented via Jersey JEE.
                    Allows long-running server svcs to free up response thread while running.
                    Provides a facility for explicitly suspending, resuming and closing client connections needs to be exposed.
                    From the client perspective connection maintained (as per synchronous I think) unless specific async submissions sent??? 
                    Where callbacks etc.. can be specified.
    [Full explanation - Must Read here](https://jersey.java.net/documentation/latest/async.html)

### Even More really good code samples
    - [LOADS of Reactive Tuturiels: http://reactivex.io/tutorials.html](http://reactivex.io/tutorials.html)
    - [RESTful api-design best practice](https://github.com/mrudul03/restful-services/blob/master/RESTful-API-BestPractices.md)
    - [MicroService Best Practice](https://github.com/mrudul03/microservices-architecture)
    - [A BitCoin implemntation](https://github.com/mrudul03/bitcoinj)
    - [And the full Application source code](https://github.com/mrudul03/microservices-demo)

### General Summary of the two examples
  Mainly Reactive eg covered here. MicroServices covered in next part.
  The Microservices uses a Spring/RESTTemplate approach. 
  Some very interesting stuff on aggregating Services (which call underying REST svcs).
  Plus it talks a lot more about the CLOUD (spring) and Config, Discovery side too.
  
  Here the examples are JEE and focussed on REST plus example of how you create responses using RxJava & AKKA.
  TODO:
    - The resume async of the respobse is an interesting topic in itself.
    - Plus how a colloection can be made observable. Does each item get emitted or individual?
      I think at once here, so not so sure of benefit at the source is a collection response from a REST get.      

### Comparison of MicroServices vs Reactive egs.
  - Spring vs JEE. MicroServices app uses SpringBoot/MVC mrather than JEE. Similar though.
  - Cloud Imple: TODO: Investigate See class ServiceApplication has @EnableCircuitBreaker, @EnableDiscoveryClientetc..
  - POJO vs Xml REST response. MicroServices POJOS from REST eg. Account, reactive return XML annotated pojos @XmlRootElement & Serializeable
                  I think this is just a Spring vs JEE differce?
  - Architecture: MicroServices splits out into REST & Corse RESTful services. TODO: Understand the reasons.
                  **Maybe its better MicroService separation OR Cloud**
  - Tech Architecture: 
                  - OAuth (spring-cloud-security)
                  - Service Discovery (Eureka)
                  - Configuration Server (spring cloud config)
                  - Monitor Dashboard (Hysterix Dashboard + Turbine)
                  - Edge Server Zuul with Load Balance (Ribbon)
                  - RESTful Spring WS for all components

## Reactive Example Notes

I think this must be deployed to Tomcat or TomEE server. I don't think embedded.
I need to understand the RxJava/Akka services further. As they similarly expose RESTful WS and returieve
                accounts etc.. BUT how does a RESTful retrieve relate to Observable pattern. Which I thought was more
                event based. ie How doe sthis work with responses and calling systems?

### Summary
  - **rest-accountinfo-service** - Uses jersey-container-servlet-core & jsp
                  Simple xml tagged domain models (DTO's). and a Restful JEE @Path("customers") controller.
                  Separates AccountInfo & list of them into sep class AccountInfoCollectionResponse. 
                  **NO PERSISTANCE logic - just mocked so model entities are just xml tagged**
  - **rest-transaction-service**. Almost identical to account service. **BUT note how sep web projects. ie Microservices**
  - **java-account-service**. Also a RESTful ws with Account & Tx.
                  - tech: spring-boot-starter-jersey, spring-boot-starter-web, jersey-container-servlet
                  - domain: Account (associates Account--List<Transaction>), AccountCollectionResponse
                                  Same as REST svcs: , AccountInfo, AccountInfoCollectionResponse, Transaction , TransactionCollectionResponse
                  - AccountResource (is controller): Calls AccountService & TcService like this:
                                  TODO: @Suspended for AsyncResponse                              
                  - Services: 
                                  AccountService: Spring @Service. Uses JEE JAX_RS 2 (from Jersey) `Client & WebTarget`.
                                                  for calling the  Account & Tx RESTful Services.
                                                  **NB. Call to the 2 REST svcs like this **
                                  AccountInfoService: 
                                  TransactionService:
  - **rxjava-account-service**: 
                  - AccountResource: Gets an Observable from AccountServive then subscribes with onCompleted() etc..
                                  `Observable<Account> account = accountService.retrieveAccount(customerNumber, accountNumber);`
                                  `account.subscribe(new Subscriber<Account>()  .....`
                                  nb. `asyncResponse.resume(account);` is used to in Subscriber to return response.
                  - AccountService: TODO: Some v interesting stuff to return the Observed accounts.
                                  Will need to understand how events are triggered & how this combines with response.
``` java
Observable<AccountInfo> accountInfo = accountInfoService
                                                  .retrieveAccountInfo(customerNumber, accountNumber, executor);
                  Observable<List<Transaction>> transcations = transactionService
                                                  .retrieveTransactions(accountNumber, executor);

                  Observable<Account> account = Observable.zip(.....
```
                  - AccountInfoService: Returns Observable, but see embeeded Subscriber & Runnable then executed.
  - **akka-account-service**: I think this performs the same as RxJava. Shown here as demo of Akka
                  TODO: Investigate
                                
### General Notes                         
This example has some really good pactices/lib choices and system design principles. These are the main ones:
  - Tech choices: JAX-RS (for REST) Spring Boot web/tomcat & DI for services.
  - System Design: 2 RESTful WS, 1 Composite WS (calls other 2). RxJava & Akka for ????
  - CompletableFuture: 2 RESTful WS return standard POJO's **BUT the CompositeService issues the httpRequest Async**
                  Returning a Future. This in turn is called by main WS AccountResource (controller) as a CompletableFuture.
                  Which allows the calls to be combined for Acount+Transaction response.
                  **This is a patern for MicroService Combination of calls**
  - Helper class for CF (**Good Pattern**) Wraps Future and Catches Exceptions
``` java
public class FutureUtil {
                public static <T> CompletableFuture<T> toCompletable(Future<T> future, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}
```            
  - AccountInfoService: check to see if target is null & re-inits. Q. Is this a feature of Endpoint going down>
  - WebTarget has a lot of builder, header etc.. around http rest requests.
  - NB. This is the Composite calling service: Which when both responses received call combine method.
                  **Should response.get() be used?** Maybe add a timeout here, if one not unresponsive.
                  AsyncResponse. Method return void. The return is somehow handled by Jersey. 
                  ** See CompleteableFutureTest v extensive & good test utilities** for lots of good testing egs with Suppliers.
``` java
CompletableFuture<AccountInfo> accountInfoResponse = 
                FutureUtil.toCompletable(accountInfoService.retrieveAccountInfo(customerNumber, accountNumber),executor);
                                //then Tx CF ....
                                //then combines
CompletableFuture<Account> response = 
                accountInfoResponse.thenCombine(transcationsResponse, (acc, trans) -> combine(acc, trans));
//Run via this:
Account account = response.get();
asyncResponse.resume(account);                                                           
```            

## MicroServices Eg
  Can run as an embedded Spring App. Although need config server & discovery server to run properly.
  So can just run AccountCoreServiceApplication for eg.
TODO
