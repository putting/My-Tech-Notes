# Should you use JPA and Hibernate in a project?

[source article](http://www.thoughts-on-java.org/use-jpa-next-project/)

[also read](https://www.quora.com/What-does-JPA-do-differently-or-better-than-JDBC
)
## Questions 

- What kind of database do you use?
                - Relational database
                - NoSQL database

- Do you use a static or dynamic/configurable domain model?
                - Static
                - Dynamic
                
- What is the main focus of your use cases?
                - Standard CRUD
                - Complex reporting/data mining
                - A little bit of both          
                
- How many highly complex queries, stored procedures and custom database functions will you have?List
                - Not that many
                - A few
                - A lot    
                
## Summary
                - **Hibernate and JPA are a great fit for applications which require a lot of CRUD operations and not too complex queries**
                - Hibernate and JPA are not a good fit for applications which require a lot of complex queries. You might want to have 
                a look at frameworks like jOOQ or Querydsl which provide better options to implement complex queries with SQL.
                - Reporting and data mining use cases require a lot of very complex queries which you can better implement 
                with SQL than with JPQL or Hibernate. You should, therefore, have a look at other frameworks, 
                like **jOOQ or Querydsl**, and don't use JPA or Hibernate.
                - JPA and Hibernate ORM are not a good fit for NoSQL databases. You should better use the specific driver for your database 
                or have a look at frameworks like Hibernate OGM.
                - The mapping definition between the database model and the data model is pretty static. JPA and Hibernate ORM are 
                therefore NOT a good fit for this project.

## QueryDSL 
                Allows sql to be built like this
                `List<Tuple> shippingAddressList = new SQLQuery(connection, new OracleTemplates())
    .from(QUsers.users)
        .join(QAddress.address) .....`
                Definitely use then approach rather than sql strings.
                A query formulated with QueryDSL, such as in the example above, returns a list of Tuples.
                
