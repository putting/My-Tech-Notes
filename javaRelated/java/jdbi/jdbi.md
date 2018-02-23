# Jdbi as used by Dropwizard

Look into v3 (released Nov 2017) soon (http://jdbi.org/)
[Summary of changes v2 -> 3](http://jdbi.org/#_upgrading_from_v2_to_v3)

## As used by xray. This uses jdbi templating to construct the sql
- store sql in *.stg file with name(paramlist) and sql.
- Uses StringTemplate3StatementLocator as shown below to inject sql
```java
return dbi.withHandle(handle -> {
            handle.setStatementLocator(StringTemplate3StatementLocator.builder(ControlCheckDao.class).build());
            return handle
                    .createQuery("asymmetricAllocations")
                    .map(new JdbiJsonResultSetMapper(objectMapper))
                    .list();
        })
```
  ```java
  asymmetricAllocations() ::= <<    //starts the template and holds the name
  SQL here...  //you can bind params also
 >> // ends here
 
 
  >>
  profitLoss(asOfDate, tradeNum, orderNum, itemNum) ::=<<
    SELECT pl_asof_date,
           pl_owner_code,
           pl_amt,
           pl_realization_date,
           pl_record_key
    FROM   pl_history
    WHERE  pl_asof_date = :asOfDate
           AND pl_secondary_owner_key1 = :tradeNum
           AND pl_secondary_owner_key2 = :orderNum
           AND pl_secondary_owner_key3 = :itemNum
>>
  ``` 
- Generic wrapper run query in dbi.
*Not sure how the sql get injected from above into the createQuery using the queryName*
```java
    private <T> List<T> runExternalQuery(String queryName, QueryConfigurator<T> queryConfigurator) {
        return dbi.withHandle(handle -> {
            handle.setStatementLocator(statementLocator);

            return queryConfigurator.configure(handle.createQuery(queryName)).list();
        });
    }
    private interface QueryConfigurator<T> {
      Query<T> configure(Query<Map<String, Object>> query);
    }
```
- Each query is run like this. 
  Note the json mapper for serialization. Diff to the strong typing used in Immutable giant dbos:
```java
    List<ObjectNode> profitLoss(ICTSTradeId tradeId, LocalDate asOfDate) {
        return runExternalQuery(
                "profitLoss",
                query -> query.bind("asOfDate", asOfDate)
                        .bind("tradeNum", tradeId.tradeNum())
                        .bind("orderNum", tradeId.orderNum())
                        .bind("itemNum", tradeId.itemNum())
                        .map(new JdbiJsonResultSetMapper(objectMapper))
        );
    }
```
