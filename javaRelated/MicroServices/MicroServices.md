# MicroServices

Promotes Reactive frameworks for MicroServices over traditional JEE

Read book: Reactive Microservices Architecture by Jonas Boner

### Distributed Systems
Up to now, the usual way to describe distributed systems has been to use a mix of technical and business buzzwords: 
asynchronous, non-blocking, real-time, highly-available, loosely coupled, scalable, fault-tolerant, concurrent, 
message-driven, push instead of pull, distributed, low latency, high throughput, etc.

###The Reactive Manifesto 
brought all these characteristics together, and it defines them through four high-level traits: 
Responsive, Resilient, Elastic, and Message driven. 

Microservices are often called stateful entities: they encapsulate state and behavior, in a similar fashion to an Object.

### MESSAGE DRIVEN 
And basically the only way to fulfill all the above requirements is to have loosely coupled services with 
explicit protocols communicating over messages. Components can remain inactive until a message arrives, 
freeing up resources while doing nothing. In order to realize this, non-blocking and asynchronous APIs must be 
provided that explicitly expose the system’s underlying message structure. 
While traditional frameworks (e.g. Spring and Java EE) have very little to offer here,
modern approaches like Akka or Lagom are better equipped to help implement these requirements

### Spring Cloud 
provides tools for Spring Boot developers to quickly apply some of the common patterns found in 
distributed systems (e.g. configuration management, service discovery, circuit breakers, intelligent routing, micro-proxy, 
control bus, and much much more).

### Microservices
• Service discovery 
• Coordination 
• Security 
• Replication
• Data consistency 
• Deployment orchestration 
• Resilience (i.e. failover) 
• Integration with other systems
