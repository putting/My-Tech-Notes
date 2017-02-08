# Technology Choices

## Questions     
                Vert.x for non-blocking I/O?
                Do I need a full blown app server for a JEE app? YES you do.

## DI Frameworks
                - Spring (Boot)
                - JEE
                
## Embedded vs Standalone server
                [Embedded containers](http://www.thedevpiece.com/embedded-containers-and-standalone-java-applications/)
                When you create a Java application, you either choose to deploy within an external servlet 
                container/application container or embed a container into your jar. 
                jars DON't need to be deployed to a container as a WAR, just run as java -jar appName
                - Tools to help
                                - SparkJava (Jetty): s a lightweight web framework which embeds a Jetty 9 into your application so you don't need to pack it as a war and deploy it in an external container.
                                - SpringBoot has been the most used tool for standalone applications.
                                - Add jetty maven plugin: `mvn clean jetty:run`
                [Good desc of pros cons](https://www.dynatrace.com/blog/the-era-of-servlet-containers-is-over/)
                                - Container versioning: 
                                - Container can share resources amongst war's: BUT ....
                                - Deployment: container (dependent on api) vs embedded (container rdeployed with app)
                                - Spring Boot good eg or embedded.
                [Good discussion with reasons](https://www.reddit.com/r/java/comments/36nt73/war_vs_containerless_spring_boot_etc/)
                                Pretty vocal in favour of embedded
                                - containerless/embedded seen as easier to build & deployed
                                - Plus embedded more suited to docker / multi instance set up.
                                - Spring Boot discussed but there is also a wildfly swarm, 
                                - Might be out of dat: NOT SO easy with Tomcat (other than Spring Boot)
                                                [see embedded tomcat how to](http://blog.sortedset.com/embedded-tomcat-jersey/)
                [Dropwizard vs Spring Boot](http://blog.takipi.com/java-bootstrap-dropwizard-vs-spring-boot/)
                                Nice summary and makes some good points.

## Logging
                - Use **logback** over others (uses slf4j under the covers)
                                Performant, plus advanced features allowing user by user logging etc..
                                
## Integration Protocols
                - xml, json
                - Apach Avro
                - REST over http
                - RPC (RMI etc...) stubs and proxies
                
## Queues & Brokers
                - MQ
                - Kafka

## Reactive (RxJava) vs CompletableFuture
                - CF allows stream like chaining of async code and callbacks.
                  Whereas RxJava is more **Event** based **PUSH DATA** rather than stream **PULL DATA**
                  Although still like a Stream of Events.

## EJB
                - Business logic. But mainly I think service layer.
                - `@PersistenceContext private EntityManager em` is used in arunGupta shopping cart eg:
                                `CatalogItemREST` to do em.persist/merge/remove(entity). So RESTful svc reads/writes etc.. via JPA
                - @Stateful: Associates with a user (httpSession normallyeg. @SessionScoped)
                - @Stateless: Taken from object pool (so not user/session related).
                                So @Singleton is definitely @Stateless as used by all users. eg RESTful Service.
                                First of all, stateless beans can have state. They can't have conversational state, which is very different.
                                You still need to protect member variables from Threads.
                                However, the SLSB (stateless EJB) should not be used to keep a state. The EJB is pooled, so you don't have any guarantee that you will return to the same instance. The SFSB is made for this purpose.
                                The EntityManager, as every instance field in the EJB, is thread safe.                       
                  
## JEE vs Spring
                -[Full JEE7 with egs- READ](https://dzone.com/refcardz/java-enterprise-edition-7)
                                - New subsystems in 7: WebSocket, Batch, Json & Concurrency.
                                - JAX-RS for RESTful Web Services: @ApplicationPath, @Path, ClientBuilder.
                                                with async paths @Suspended & sync with @PathParam
                                                Filters for modifying request/response headers.
                                - Servlet 3.1: `@WebServlet("/report") public class MoodServlet extends HttpServlet`
                                                and now non-blocking
                - [JEE Summary](https://dzone.com/articles/java-ee-basics)
                                v good overview, showing cxontainers etc..
                                - Set of Standard Api's implemented by diff vendors.
                                - Containers: EJB, Web, App CLient & Applet which provide 'services' to apps. Interface between App Server & app
                                                egs. Services provided by container: Persistance (JPA), ORM. Messaging (JMS). Contexts & DI (CDI). JTA.
                - JEE example: [shopping cart arungupta](http://blog.arungupta.me/monolithic-microservices-refactoring-javaee-applications/) 
                                - He uses jboss wildfly as app server
                                - JEE libs: javax.javee-api, rg.jboss.resteasy.resteasy-client, resteasy-jaxb-provider
                                - **javee-api** is the main JEE lib with annotations, ejb, ws, xml, inject, jms, persistance etc..
                                - Annotations:
                                                - @SessionScoped (javee-api)
                                                - @XmlRootElement (javax.xml.bind.annotation). part of jdk
                                                - @Stateless, @Singleton, @Startup (javee-api/ejb) **Want to undesrtand more fully**
                                                                [RESTful services are stateless](http://www.infoworld.com/article/2946856/application-architecture/best-practices-in-using-restful-services.html)
                                                - REST stuff: @Path, @POST, @GET, @Consumes, @Produces (javee-api/javax.ws.rs)
                                                - @Entity, @Id, @Columns, @NamedQuery(JPA) etc: (javee-api/persistance)
                                                - @Inject (javee-api/inject)
                                - `JsonObject jsonObject = Json.createObjectBuilder()` (javee-api/json)
                                - @ZooKeeperServices: jBoss wildfly
                - [Good Blog discussion](https://www.reddit.com/r/java/comments/39c09l/why_are_people_still_choosing_spring_for_new/)
                                Correct me if I'm wrong, but JAX-RS + Angular is pretty much the same of @RestController + Angular 
                                and nowhere near Spring data rest that basically exposes your JPA entities as discoverable REST resources.
                                - Remember Spring Data uses Repositories to manage CRUD stuff.
                                - **Tomcat JEE Good but maybe Spring Boot (MVC etc.) provides more out of the box.**
                - [dump jee](https://dzone.com/articles/why-should-we-dump-java-ee)
                                - Fewer jee servers all the time. Spring can run in any runtime env.
                                - So better to choose Jetty, Tomcat, WildFly, TomcatEE and Spring with required apis only.
                                - No need to have those JEE runtime environments like JBoss, Weblogic or Websphere.
                                - Spring application can be run in a full-fledged JEE server as well as in lightweight JSP containers like Jetty, Tomcat or Netty, 
                                                avoiding big overhead costs.  Spring can even run in standalone mode as the Spring Boot module can wrap a Jetty or Tomcat.
                                                
## WebSockets
                - [trsha gee sense app](https://github.com/trishagee/sense)
                                No Spring or JEE. Just Jetty.
                                - Websockets **server** using jetty. org.eclipse.jetty.websocket.javax-websocket-server-impl
                                - json org.glassfish.javax.json
                                - Websocket: javax.websocket.javax.websocket-api
                                - javaFx
                                - Implements Endpoint @javax.websocket.server.ServerEndpoint("serverBroadcast") which is a websocket-api * @javax.websocket.ClientEndpoint
                                - @FunctionalInterface public interface MessageHandler<T>. **Used to pass in behaviour as lambda**
                                - @FunctionalInterface public interface MessageListener<T> {void onMessage(T message);
                                - Service<T> implements Runnable
                                
                                
## Jetty vs Spring Boot
                - Spring Boot for RESTful (MVC) svc, with embedded jetty/tomcat.
                                
## ORMs
                - Hibernate
                - JPA (part of JEE)

## DB's vs NoSQL
                - RDBMS
                - Mongo
                - Cassandra

## Containers
                - Docker (Container)
                                - [Good Recipes](http://blog.arungupta.me/9-docker-recipes-javaee-applications-techtip80/)
                - ZooKeeper (Service Registration & Discovery)
                                - [ZooKeeper](http://blog.arungupta.me/zookeeper-microservice-registration-discovery/)
                
## App Servers (needed for JEE) vs Servlet Containers (or embedded)
                - App servers can implement the FULL profile, the web profile or just web server portion (servlet 3.1)
                                [see what jetty supports from web profile](https://www.eclipse.org/jetty/documentation/9.3.x/jetty-javaee.html)
                - [App servers are dead](https://www.beyondjava.net/blog/application-servers-sort-of-dead/)
                                I don’t think it’s a good idea to use a full-blown **JavaEE application server**. 
                                I prefer to use a **simple servlet container** such as Tomcat or Jetty and to add the jar I want to use
                                Better to just embedd jetty in main app. Describes how to embed browser in app (for devs).
                                Better to deploy to VM rather than many svcs in 1 app server
                - Prefer simple cmd line app deployment packaged as jar (with embedded jetty if web)
                -[Prefer Docker to app server](https://blog.fabric8.io/the-decline-of-java-application-servers-when-using-docker-containers-edbe032e1f30#.um6dxddh1)
                - jetty, netty These app or just lightweight http/servlet containers, also run the Java EE 7
                - TomEE is a full JEE spec server
                - wildFly from RedHat pops up with Docker. Look into if good?

## UI Dev
                - javascript
                - angularJS
                - template frameworks
                - javaFx
                
## Caching
                - memcache
                - hazelcast etc..
                
## Test Framesworks
                - jmh?
                - Mockito
                
## Build
                - Maven
                - Gradle
                
## CI & CD platforms
                - Jenkins
                - TeamCity
                
## Code Repo
                - svn
                - git
                
## Distributed Compute
                - Spark

                                
