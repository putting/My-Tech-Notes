# Bulls First Trading App DDD eg
[https://archfirst.org/bullsfirst/](https://archfirst.org/bullsfirst/)

[Main DDD pages](https://archfirst.org/domain-driven-design/)

Uses JEE I think with Java 7 unfortunately. JBoss glassFish as app server.
In many ways its a conventional setup, but rather detailed.

## Overview
At a very high level, users can enter orders to buy and sell securities through the front-end. 
The orders are managed by an Order Management System (OMS), which is typically owned by a brokerage firm. 
The OMS places the orders in an Exchange where a matching engine matches them and executes trades.
The general rule of thumb is to group together elements that are related and form a natural function.
Hence broken Bullsfirst into two bounded contexts, each with its own model:
                - Bullsfirst Order Management System (OMS): Responsible for order creation, maintenance and reporting
                - Bullsfirst Exchange: Responsible for order matching and execution

**TODO:** Understand the exchange part. Which appears to be acting as the source of events.
Need to understand its real purpose as the OMS persists entities from UI etc..
this appears to be back-end
I probably require a better overview of the interaction between svcs & apis. The tech design doc 
appears to be missing. 
- Using publishing for events. I think this is part of CQRS Events which publish when state changes.
                and are used **to communicate between Bounded Contexts.**

## Layered Architecture
                [interesting diagram](https://archfirst.org/domain-driven-design/6/)
                - User Interface: Accepts user cmds, present info to user. No necessarily 'users'. see diagram
                - Application: Manages tx, translates DTO's, coordinates activities, creates and accesses domain objects.
                - Domain: Contains the state and behaviour of the domain.
                - Infrastrucure: Supports other layers, includes repos, adapters, frameworks etc..
                
## My review of some classes
                - MatchingEngine.performMatching(): This method could really benefit from java 8 processing.
                                Nested for's and if's not pretty. Same goes for matchOrder method.

## Domain. NB. This is A DDD example. (TODO: I think these all exchange Entities)
                These Domain items in bfoms-common-domain are full JPA entities.
                - Headers: @XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "Order") @Entity @Table(name="Orders")
                - All inherit from `DomainEntity` which have `@XmlElement(name = "Id") protected Long id;` 
                `@XmlTransient protected int version;` and are serializeable
                - **Order** related classes ( a lot)
                                Order(main attribs and many @Transient methods),  OrderCreated, OrderCriteria, OriderEstimate,  
                                Enums: OrderSide(buy/sell), OrderStatus(new, filled, cancelled ...), OrderCompliance, OrderTerm, OrderType (Market, Limit)
                - **Account**: BaseAccount(@Entity, name, status, parties & txs), BrokerageAccount (@Entity), BrokerageAccountAce, BrokerageAccountFactory (create BA with diff permissions), BrokerageAccountRepository, 
                                BrokerageAccountService(is a svc, manages repos and call to other svcs), BrokerageAccountSummary (not entity but @XmlType), AccountParty
                - **Brokerage** related: Lot, Position(not entity bbut @XmlType), SecuritiesTransferAllocation, 
                                Trade->Transaction(entity & xml), CashTransfer->Transaction(, SecuritiesTransfer->Transaction
                                TradeAllocation
                - **Ref Data**: Instrument, ReferenceDataService (get instruments from external svc, `private volatile List<Instrument> instrumentList;` and sync)
                - **Security Related**: As in system security not producrs Party, Person etc..
                - Interesting approach to where constructors, commands, queries, gettere/setters go. I agree that
                                getter/setters should be last (with eqals & hashcode)
                - Ideally I would like to have no setters in my entities (all changes should be made through commands), 
                but practically, I was not able to do it due to possible limitations in Java Persistence API (JPA) 
                (field-based access was triggering compilation errors). As a compromise, I have made **all the setters private.**
                - **Domain Services:** Some aspects of the domain are not easily mapped to objects. The best practice is to declare these as Domain Services. Domain Services are distinct from Application Services; unlike 
                Application Services, they do contain business logic. eg `MatchingEngine`
                This logic does not naturally belong inside the Order because we need to apply this logic **across many orders.**
                A good Domain Service has three characteristics:
                                - The operation relates to a domain concept that is not a natural part of an Entity or a Value Object.
                                - The interface is defined in terms of other objects in the domain model
                                - The operation is stateless.
                - **Domain Event**
                                A Domain Event is a simple object that represents an interesting occurrence in the domain. 
                                Domain Events help decouple various components of an application.

## Security
                - Basic Auth used and a /secured/ uri.
                - @Context  javax.ws.rs.core.SecurityContext to store principal 
                - GetUser Note that there is no concept of user sessions in the **stateless** REST architecture. In that vein, this 
                is a stateless request which is used simply to authenticate the user and capture their credentials for subsequent requests.
                Q. Does client then provide User as header on subsequent requests?

## RESTful notes

### Endpoints
                - UsersResource, OrderResource, AccountsResource etc..
                All with headers: @Stateless @Path("/users") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)    
                - Link to resource in Response (I like this as it tells UI the link for created resource)
                                `Link self = new Link(uriInfo, request.getUsername());
        return Response.created(self.getUri()).entity(self).build();`
                - javax.ws.rs.core.URIInfo used for link and uses @Context annotation.
                - Typical @POST & @GET. As JEE returns javax.ws.rs.core.Response which defines HTTP response codes.
                - GetBrokerageAccounts: Returns summary infoabout all brokerage accounts for which the requesting 
                user has View permissions. Each summary object is a hierarchical object consisting of some general 
                information along with a combination of **instrument and cash positions** Instrument positions are further broken down to lot positions.
                - GetOrders: Returns orders matching zero or more of the following criteria. Each criterion is defined as a query parameter.
                                accountId, symbol, fromDate etc..
                - GetTransactions: Returns transaction summaries.
                - Instruments: Note that instrument information is served from the exchange (http://apps.archfirst.org/bfexch-javaee/).
                - Market Prices: Note that market prices are served from the exchange (http://apps.archfirst.org/bfexch-javaee/).

### Domain DTO's in RESTful (These are mirrors of the real Exchange Domain entities)
                - User, Order, Account: All are POJO DTO's. **No annotations, equals or hashcode**
                - The json un/marshall is done `org.dozer.Mapper` Q. Not sure why used?
                
## WebService & Transaction Service (SOAP JAX-WS impl - maybe as communicating with silverlight UI?)
                These both appear to be stand-alone from REST svc.
                - TradingWebService has javax.jws.@WebService (including wsdl)  & I think used to communicate with Silverlight.
                Also has javax.jws.@WebMethod and @WebResult.
                - TradingTxnService has all service calls and is just called by TradingWebService.
                - [REST vs SOAP article dzone](https://dzone.com/articles/web-services-architecture)
                - **JAX-WS**: Web Services using XML and SOAP [good desc](http://docs.oracle.com/javaee/6/tutorial/doc/bnayl.html)
                - **JAX-RS**: RESTful Web Services using json (and maybe xml)[same oracle site](http://docs.oracle.com/javaee/6/tutorial/doc/giepu.html)

## CQRS
                Recommends that every method should **either** be a Command that performs an action, or a Query that returns data to the caller, but not both.
                - Commands: tell bounded context to do something
                - Queries: ask for information from it
                - Events: a Bounded Context generates events to inform that something happened inside it.
                
## Exchange

### Event Publishing
                - DefaultMarketDataEventPublisher; marketPriceChangedEvent.fire(event)
                - ReferenceDataWebService & MarketDataWebService are SOAP WS.
                
### Scheduled jobs
                - EndOfDayScheduler;  @Schedule(hour="16", minute="00", timezone="America/New_York")

                
## JMS Exchange Listeners (simple single class impls)
Two simple jms listeners which call services. NB. The ejb Part?
                - MarketPriceListener implements javax.jms.MessageListener. javax.ejb.@MessageDriven
                                Implements onMessage and calls `marketDataService.updateMarketPrice(marketPrice);`
                                Uses xml msg MarketPrice
                - ExchangeMessageListener @MessageDriven(mappedName="jms/ExchangeToOmsJavaeeQueue")
