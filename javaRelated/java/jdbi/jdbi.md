# Jdbi as used by Dropwizard

## As used by xray
- store sql in *.stg file with name(paramlist) and sql.
  ```java
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
- Generic wrapper run query in dbi
```java
    private <T> List<T> runExternalQuery(String queryName, QueryConfigurator<T> queryConfigurator) {
        return dbi.withHandle(handle -> {
            handle.setStatementLocator(statementLocator);

            return queryConfigurator.configure(handle.createQuery(queryName)).list();
        });
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
