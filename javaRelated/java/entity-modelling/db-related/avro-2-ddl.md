# Convert Avro schema to DDL to create a table

```java
/**
 * Utility for generating a DDL file suitable for persisting a record from an Avro schema. This can be used to
 * generate initial Flyway scripts, or to assert that the schema created by Flyway scripts matches the schema
 * of the Avro objects. See com.mercuria.vault.consumer.avro.DatabaseSchemaCompatibleTest for an example.
 */
public class AvroSchema2DDL {

    private String targetSchema;
    private Map<String, String> additionalColumns = Collections.emptyMap();
    private List<String> additionalPrimaryKeyColumns = Collections.emptyList();
    private boolean useDbColumnName = false;
    private Map<String, Set<String>> mappedFields;
    private boolean indexInsteadOfPrimaryKey;

    public AvroSchema2DDL() {
    }

    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
    }

    public void setAdditionalColumns(Map<String, String> additionalColumns) {
        this.additionalColumns = additionalColumns;
    }

    public void setAdditionalPrimaryKeyColumns(List<String> additionalColumnsKeys) {
        this.additionalPrimaryKeyColumns = additionalColumnsKeys;
    }

    public void setUseDbColumnName(boolean useDbColumnName) {
        this.useDbColumnName = useDbColumnName;
    }


    public void setIndexInsteadOfPrimaryKey(boolean indexInsteadOfPrimaryKey) {
        this.indexInsteadOfPrimaryKey = indexInsteadOfPrimaryKey;
    }

    /**
     * Identifies record schemas within the replication schema, and creates tables for them.
     * @param replicationSchema
     * @return
     */
    public List<String> createTables(Schema replicationSchema) {
        List<Schema> topLevelTypes = replicationSchema.getTypes();
        Schema replicationBatch = topLevelTypes.stream()
                .filter(topLevelType -> topLevelType.getName().equals("ReplicationBatch"))
                .findFirst()
                .orElseThrow(() -> new AvroException("Replication schema contains no ReplicationBatch record"));
        Schema.Field itemsField = getField(replicationBatch, "items");
        if (itemsField == null || itemsField.schema().getType() != Schema.Type.ARRAY) {
            throw new AvroException("Replication schema field ReplicationBatch.Items is not an array");
        }
        List<Schema> updateRecordTypes;
        if (itemsField.schema().getElementType().getType() == Schema.Type.RECORD) {
            updateRecordTypes = Collections.singletonList(itemsField.schema());
        } else if (itemsField.schema().getElementType().getType() == Schema.Type.UNION) {
            updateRecordTypes = itemsField.schema().getElementType().getTypes();
        } else {
            throw new AvroException("Replication schema field ReplicationBatch.items is not an array of records or union");
        }

        Set<String> updateRecordNames = updateRecordTypes.stream().map(s -> s.getFullName()).collect(Collectors.toSet());
        List<Schema> updateRecords = topLevelTypes.stream()
                .filter(topLevelType -> updateRecordNames.contains(topLevelType.getFullName()))
                .collect(Collectors.toList());
        return updateRecords.stream()
                .flatMap(updateRecord -> updateRecord.getFields().stream().filter(field -> field.name().equals("data")))
                .map(dataField -> createTable(dataField.schema()))
                .collect(Collectors.toList());

    }

    /**
     * Returns a "CREATE TABLE" DDL statement which is able to store rows defined by the provided Avro schema.
     *
     * @param recordSchema
     * @return
     */
    public String createTable(Schema recordSchema) {
        StringBuilder result = new StringBuilder();
        String tableName = recordSchema.getName();
        String qualifiedTableName = targetSchema != null ?  targetSchema + "." + tableName : tableName;
        result.append("CREATE TABLE ").append(qualifiedTableName).append(" (\n");
        List<String> primaryKeys = new ArrayList();
        for (Schema.Field field : recordSchema.getFields()) {
            String columnName = columnName(field);
            if (additionalColumns.containsKey(columnName) || !isFieldMapped(recordSchema.getName(), field.name())) {
                continue; // Caller overrides column definition
            }
            Schema fieldSchema = field.schema();
            if (isPrimaryKey(field)) {
                primaryKeys.add(columnName);
            }
            Set<Schema> types = new HashSet<>();
            if (fieldSchema.getType() == Schema.Type.UNION) {
                types.addAll(fieldSchema.getTypes());
            } else {
                types.add(fieldSchema);
            }
            Map<Boolean, List<Schema>> nullTypes = types.stream().collect(Collectors.partitioningBy(type -> type.getType() == Schema.Type.NULL));
            boolean nullable = !nullTypes.get(true).isEmpty();
            List<Schema> nonTypeTypes = nullTypes.get(false);
            if (nonTypeTypes.isEmpty()) {
                throw new AvroException("Column " + columnName + " of " + tableName + " has unknown type");
            }
            if (nonTypeTypes.size() > 1) {
                throw new AvroException("Column " + columnName + " of " + tableName + " has ambiguous type: " + types);
            }
            Schema realFieldSchema = nonTypeTypes.iterator().next();
            AvroType type = getByFieldSchema(realFieldSchema);
            result.append("\t").append(columnName).append(" ").append(type.getSqlType(field, realFieldSchema));
            if (!nullable) {
                result.append(" NOT NULL");
            }
            result.append(",\n");
        }

        additionalColumns.forEach((name, definition) ->
                result.append("\t").append(name).append(" ").append(definition).append(",\n"));

        primaryKeys.addAll(additionalPrimaryKeyColumns);

        if (!indexInsteadOfPrimaryKey) {
            result.append("\n\tCONSTRAINT PK_").append(tableName)
                    .append(" PRIMARY KEY (")
                    .append(primaryKeys.stream().collect(Collectors.joining(", ")))
                    .append(")");
        } else {
            // Remove last ,\n
            Preconditions.checkState(result.toString().endsWith(",\n"));
            result.setLength(result.length() - 2);
        }
        result.append("\n)");
        if (indexInsteadOfPrimaryKey) {
            result.append(";\n");
            result.append("\n${IF_MSSQL} CREATE CLUSTERED INDEX PK_").append(tableName).append( " ON ").append(qualifiedTableName).append(" (")
                    .append(primaryKeys.stream().collect(Collectors.joining(", ")))
                    .append(")");
            result.append("\n${IF_H2} CREATE INDEX PK_").append(tableName).append( " ON ").append(qualifiedTableName).append(" (")
                    .append(primaryKeys.stream().collect(Collectors.joining(", ")))
                    .append(")");
        }
        return result.toString();
    }

    private boolean isPrimaryKey(Schema.Field field) {
        return Boolean.TRUE.equals(field.getObjectProp("db-primary-key")) ||
                Boolean.parseBoolean(field.getProp("db-primary-key"));
    }

    private String columnName(Schema.Field field) {
        if (useDbColumnName) {
            String dbColumnName = field.getProp("db-column");
            if (dbColumnName != null) {
                return dbColumnName;
            }
        }
        return field.name();
    }

    public void setMappedFields(Map<String, Set<String>> mappedFields) {
        this.mappedFields = mappedFields;
    }

    public boolean isFieldMapped(String schemaName, String fieldName) {
        if (mappedFields == null) {
            return true;
        }
        Set<String> fields = mappedFields.get(schemaName);
        if (fields == null) {
            return true;
        }
        return fields.stream().anyMatch(fieldName::matches);
    }
}
```
