# Modelling Db fields and tables

## Specifically where entities are Uniquely keyed and change over time
There are some good principals here for java/db modelling with keys/temporal/audit etc...

### Domain Immutable classes mimicing the db tables but implementing the Entity
```java
@Jdbi(tableName="VAULT.InventoryPosition")
@Value.Immutable
public interface InventoryPosition extends VaultEntity<InventoryPositionKey, InventoryPosition> {
```

### Generic field modelling and uses
```java
public interface VaultEntityKey {
    List<FieldData> fields();
}

public interface VaultEntity<K extends VaultEntityKey, E> {
    K key();
    AuditInfo auditInfo();
    E withAuditInfo(AuditInfo auditInfo);
}

public class FieldData {

    private final Object value;
    private final String name;
    private final Class<?> type;

    public FieldData(String name, Object value, Class<?> type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }
  ...getters

    public Class<?> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
    ....
    }

    @Override
    public int hashCode() {
    .....
    }
}
@Value.Immutable
public interface TransactionInfo {

    /**
     * Creates a TransactionInfo object that represents a local Vault timestamp for entities which are not loaded
     * from another source system.
     */
    static ImmutableTransactionInfo newLocalTransactionInfo(Instant now) {
        return ImmutableTransactionInfo.builder()
                .sequence(0)
                .transId(0)
                .timestamp(now)
                .insertTime(now)
                .build();
    }

    //Replication message sequence number
    long sequence();
    
    //ICTS transaction id (not necessarily strictly sequential)
    long transId();
    
    //ICTS timestamp (not necessarily strictly sequential)
    Instant timestamp();
    
    //System time
    Instant insertTime();

}
@Value.Immutable
@Value.Style(allParameters = true)
@Jdbi
@JsonSerialize(as = ImmutableAuditInfo.class)
@JsonDeserialize(as = ImmutableAuditInfo.class)
public interface AuditInfo {

    ImmutableAuditInfo DUMMY_AUDIT_INFO = ImmutableAuditInfo.builder()
            .sequenceNo(0)
            .latest(true)
            .transId(0)
            .inTime(MAX_TIME)
            .outTime(MAX_TIME)
            .insertTime(MAX_TIME)
            .build();

    static AuditInfo newLocalAuditInfo(Instant now) {
        return of(TransactionInfo.newLocalTransactionInfo(now));
    }

    static ImmutableAuditInfo of(TransactionInfo transactionInfo) {
        return ImmutableAuditInfo.builder()
                .sequenceNo(transactionInfo.sequence())
                .transId(transactionInfo.transId())
                .insertTime(transactionInfo.insertTime())
                .inTime(transactionInfo.timestamp())
                .outTime(MAX_TIME)
                .build();
    }

    @JdbiOptions(ignore = true)
    default AuditInfo withDeletionAt(TransactionInfo transactionInfo) {
        return ImmutableAuditInfo.builder()
                .from(this)
                .outTime(transactionInfo.timestamp())
                .updateTime(transactionInfo.insertTime())
                .updateTransId(transactionInfo.transId())
                .updateSequenceNo(transactionInfo.sequence())
                .build();
    }


    long sequenceNo();
    long transId();
    Instant inTime();
    Instant outTime();
    Instant insertTime();

    @JdbiOptions(ignore = true)
    @Value.Default
    default boolean latest() {
        return false;
    }

    // Attributes that describe when this row version was deleted
    // We don't currently persist this, but could.

    @JdbiOptions(ignore = true)
    @Nullable
    Instant updateTime();
    @JdbiOptions(ignore = true)
    @Nullable
    Long updateTransId();
    @JdbiOptions(ignore = true)
    @Nullable
    Long updateSequenceNo();

    /**
     * Returns information about the transaction which created this entity version.
     */
    @JdbiOptions(ignore = true)
    default TransactionInfo transactionInfo() {
        return ImmutableTransactionInfo.builder()
                .sequence(sequenceNo())
                .transId(transId())
                .timestamp(inTime())
                .insertTime(insertTime())
                .build();
    }

    /**
     * Returns information about the transaction which deleted/superseded this entity version.
     */
    @JdbiOptions(ignore = true)
    default TransactionInfo deletionTransactionInfo() {
        return ImmutableTransactionInfo.builder()
                .sequence(updateSequenceNo())
                .transId(updateTransId())
                .timestamp(outTime())
                .insertTime(updateTime())
                .build();
    }

}
```
