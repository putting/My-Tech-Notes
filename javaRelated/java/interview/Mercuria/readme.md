# Mercuria Interview Questions

## Sharing code
https://codeshare.io/

## Initial Java Questions
- Explain how a HashMap workins in Java. Follow up on what makes a good key. Is it threadsafe?
- What do you understand by Immutability? How do you make a class immutable?
- Do you know what a java record is? And what is it used ofr?
- What do you understand by 'Memory is managed in Java' meen? What is the java Memory Model? https://www.baeldung.com/java-memory-management-interview-questions
  - What is GC and its advantages/disadvantages?
  - What are the Stack (nested method calls and local vars + refs to objs on heap) and Heap(objectes created. NB. ref is on stack)
  - What is Generational GC: Young Gen (eden S0 & S1), Old Gen, Perm Gen (metadata desc classes).
- Spring What is IOC? : The container manages the lifecycle of the objs and dependencies. Its a form of decoupling code config from code logic.
- Microservices: Ask what are the advantages and disadvantages of this Architectural approach. Pros: Release cycle, smaller easier components to reason with dedicated db's?, Cons: Chatty?

- ✅ Pros of Microservices

1. Scalability
    Each service can be scaled independently, which is more efficient and cost-effective than scaling an entire monolithic app.
2. Flexibility in Tech Stack
    Teams can use different programming languages, databases, and tools for each service, which allows choosing the best tech for each specific use case.
3. Faster Development & Deployment
    Smaller, independent teams can build, test, and deploy their services without waiting on others, increasing overall speed of development.
4. Better Fault Isolation
    If one microservice fails, it doesn’t necessarily bring down the whole system (with proper design), improving reliability.
5. Easier Maintenance & Updates
    Services are smaller and focused on specific functionality, making them easier to understand, maintain, and refactor.
6. Enables Continuous Delivery
    Services can be deployed independently, so updates to one service don't require full redeployment of the app.

❌ Cons of Microservices

1. Increased Complexity
    Managing many services, especially inter-service communication, configuration, and deployment, is more complex than dealing with a monolith.
2. Network Latency
    Communication between services typically happens over the network (e.g., HTTP, gRPC), introducing potential latency and failures.
3. Data Consistency Challenges
    With separate databases, maintaining transactional consistency (ACID) across services is hard. You often need to settle for eventual consistency.
4. DevOps Overhead
    Requires a mature DevOps culture and tooling—think CI/CD pipelines, container orchestration (like Kubernetes), service discovery, centralized logging, monitoring, etc.
5. Harder Debugging & Testing
    Debugging across multiple services is harder than in a single monolithic app. End-to-end testing becomes more complex.
6. Deployment Complexity
    You’ll need to manage versioning, backwards compatibility, and dependencies across services.

## CV if interesting
- Kafka
  - What format do you use for messages?
  - How do you manage schema evolution?
  - See kafka in-depth ques: https://github.com/putting/My-Tech-Notes/blob/master/javaRelated/java/interview/Mercuria/Kafka.md
- gRPC https://github.com/putting/My-Tech-Notes/blob/master/javaRelated/java/interview/Mercuria/grpc.md
  - How would you secure a gRPC service? eg with OAuth? Add a Server Interceptor with a Spring AuthenticationManager with BearerAuthenticationReader for eg.
  - Best paractices:
    -Keep your message types small and focused
    -Avoid using gRPC for one-off requests
    -Batch requests together whenever possible
    -Use protobufs to define your message types
    -Use the gRPC gateway for external access to your gRPC services

## Simple coding algoithms
- FizzBuzz: https://github.com/putting/My-Tech-Notes/blob/master/javaRelated/java/interview/Mercuria/FizzBuzz.md
- 2nd largest number in Array: https://github.com/putting/My-Tech-Notes/blob/master/javaRelated/java/interview/Mercuria/2nd%20largest%20number%20in%20array.md

## Code smells 
- Bad code eg: https://github.com/putting/My-Tech-Notes/blob/master/javaRelated/java/interview/Mercuria/Simple%20Bad%20Java%20Code%20Exercise.md

## Database 
- Simple Order: https://github.com/putting/My-Tech-Notes/blob/master/javaRelated/java/interview/Mercuria/SQL%20Simple%20Order.md


## Hard Coding
- Travelling Salesman: https://github.com/putting/My-Tech-Notes/blob/master/javaRelated/java/interview/Mercuria/Travelling%20Salesman.md
