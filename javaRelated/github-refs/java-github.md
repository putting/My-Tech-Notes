##1 Java

###1.1 Tomasz Nurkiewicz
- **Author:** [Tomasz Nurkiewicz](http://www.nurkiewicz.com/) Good website with lots of articles.
- [http://www.nurkiewicz.com/](http://www.nurkiewicz.com/)
- **Summary:** Lots of good articles on many java subjects
                                - `BaseTestObservables`: Interesting use of Interface with default methods for test helpers.
                                - `Helpers`: Nice use of CountdownLatch for waiting for async threads to finish in test. Instead of *join*.
                
### 1.1.1 Download Server (5 pages)
- [website](http://www.nurkiewicz.com/2015/06/writing-download-server-part-i-always.html) 
- [github](https://github.com/nurkiewicz/download-server)
- **Technology:** Spring REST (MVC), Groovy (tests), HTML tips, Hashcode headers, MockMvc (spring test)
- **Summary:** The objective is to stream very large files with the minimum of memory usage.
                - Uses Spring @RestController & @SpringBootApplication.
                - Worth looking at the `FilePointer.java` abstractions and why he chose this approach. Including `FileSystemPointer`
                                which wraps file using `BufferedInputStream`.
                - Use of UUID for masking real resource names.
                - Spring `ResponseEntity` & `Resource` used.
                - Lots of discussion on headers and ETag (interesting)

###1.2 JEE Monolithic to Microservices Refactoring for Java EE Applications
- **Author:** [arungupta](http://blog.arungupta.me/monolithic-microservices-refactoring-javaee-applications/) More articles.
- [github monolithic](github.com/arun-gupta/microservices/tree/master/monolith/everest)
- [github microservice](https://github.com/arun-gupta/microservices/blob/master/microservice)
- **NB.** Make sure I get the MicroService code with separate modules, including services, everest, uzer, utils, webapp. Has readme.adoc which contains instructions for running zookeeper, wildFly and maven. Didn't build correctly at work.
- **Summary:** Important I go over this, esp JEE annotations. Shows an eg of breaking up a monolith into MicroService. Compares lists of files against each other, incl jar, war etc.. **Docker** eg. V important I look through. 
- **Technology:** JEE, Docker,jBoss, netty, widfly(?)
- Other articles:
                - [9 Docker Recipes](http://blog.arungupta.me/9-docker-recipes-javaee-applications-techtip80/)
                - [Mico, Mono and No Ops](http://blog.arungupta.me/microservices-monoliths-noops/)


###1.3 Blogging Example using Spring
- **Author:** [not sure tidyjava?](http://tidyjava.com) .
- **Summary:** Need to find on GitHub blogging-platform, uses Gradle. Has some javascript too. Not sure if relevant

###1.4 Twitter Blogging eg of MicroServices (see video)
- **Author:** [mechanitis](http://bbc.co.uk) . Not sure. find
- **Summary:** Follow video. Good egs here and ideas.
- **Technology:** WebSockets, Twitter stream, javaFx, Groovy. Good egs of endpoints and listeners.

###1.5 Vending machine Design
- **Author:** [javarevisited part1](http://javarevisited.blogspot.co.uk/2016/06/design-vending-machine-in-java.html) . [part2](http://javarevisited.blogspot.co.uk/2016/06/java-object-oriented-analysis-and-design-vending-machine-part-2.html) adds testing and design doc
- **Summary:** Good discussion and ideas. Look at the comments at the bottom.
- **Technology:** Java. Its about Dersign approach mainly
