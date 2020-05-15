# Java Stuff to Remember

## JDK11 Features vs 8
see https://codete.com/blog/java-8-java-11-quick-guide/
- modules
- Immutable collections. Set.of("John", "George", "Betty") or Map.of("John", 1, "Betty", 2)
- var for local variables without type
- jshell REPL
- private methods in interfaces
- Optional.ifPresentOrElse() & Optional.Stream()


### MUST Read
  - [Banking eg of Rx, MS & REST](https://github.com/putting/Documentation/blob/master/javaRelated/java/Reactive/banking-eg-rx-aka-ms.md)
  - [DDD BullsFirst eg good - see noted](https://github.com/matthewjosephtaylor/java-blockchain)

### Use These Libraries
  - Mockito: [link](https://dzone.com/articles/mocking-the-unmockable-the-mockito2-way)
  - logback: 
  - Swagger & HAL: Use these for documenting APIs with lots of extras around discovery
  - mountebank: first open source tool to provide cross-platform, multi-protocol test doubles over the wire. TODO: Investigate [http://www.mbtest.org/](http://www.mbtest.org/)
  - Vagrant: Can be used to vreate virtual Cloud on PC
  - Docker: Is a PaaS
  - Puppet Chef and Ansible: Helps with config & Deploying artifacts.
  - Rx Observalbles (Marbles): Good visual way of displaying functionality [http://rxmarbles.com/](http://rxmarbles.com/)
  - Structre 101 & SchemaSpy: Graphic display of java package dependencies. Schema spy does same for dB tables.
    [http://schemaspy.sourceforge.net/](http://schemaspy.sourceforge.net/)
  - Vert.x (async) vs Tomcatv(http) vs Jetty
  - Vmlens: tool to detect race conditions
  - Jooq: Provides a DSL for complex **SQL** querying and can return Record objects.
  - Spark Web Framework: Good for simple web sites. [sparkjava.com]( sparkjava.com)
  - httpie: This is a user friendly curl alternative. eg `http -v localhost:8080` [https://httpie.org/](https://httpie.org/)
  - LogRotate, Logstash or splunk: for monitoring
  - Apache commons ImmutablePair
  
### Points of Interest
  - You can get the version of software deployed (compiled) like this: Utils.class.getPackage().getImplementationVersion();
    Where Utils is just a class the class this code is contained in. Make available through REST

[//]: # (Contains links only and removed by processor)

[bbc]: <http://bbc.co.uk>
