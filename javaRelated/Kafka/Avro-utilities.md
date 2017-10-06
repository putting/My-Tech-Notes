# Avro Utilities

- Mapping from a sql schema to Avro
- Serializing & De-serializing avro schema - **good** look
- MsgBuilder
- ResultSet to Avro

## Mapping from a sql schema to Avro

```java
package com.abc.dali.avro.tools;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.CaseFormat;
import com.mercuria.dali.avro.DaliAvroException;
import com.mercuria.dali.avro.util.AvroType;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class DDL2AvroSchema {

    private final DataSource dataSource;
    private final String catalog;
    private final String schemaPattern;
    private final String tablePattern;
    private final String avroNamespace;

    private Predicate<String> tablePredicate = tableName -> true;

    public DDL2AvroSchema(DataSource dataSource, String catalog, String schemaPattern, String tablePattern,
                          String avroNamespace) {
        this.dataSource = dataSource;
        this.catalog = catalog;
        this.schemaPattern = schemaPattern;
        this.tablePattern = tablePattern;
        this.avroNamespace = avroNamespace;
    }

    public String getDataSchema() {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            write(buffer);
            return buffer.toString("utf-8");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SQLException e) {
            throw new DaliAvroException("Unexpected database error", e);
        }
    }

    private static String toUpperCamelCase(String columnName) {
        char[] chars = toLowerCamelCase(columnName).toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    private static String toLowerCamelCase(String columnName) {
        if (columnName.contains("_")) {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, columnName);
        } else {
            char[] chars = columnName.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            for (int i = 1; i < chars.length && (i + 1 >= chars.length || Character.isUpperCase(chars[i + 1])); i++) {
                chars[i] = Character.toLowerCase(chars[i]);
            }
            return new String(chars);
        }
    }

    public void write(OutputStream out) throws IOException, SQLException {

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(out, JsonEncoding.UTF8);
        jsonGenerator.useDefaultPrettyPrinter();

        jsonGenerator.writeStartArray();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            try (ResultSet tables = databaseMetaData.getTables(catalog, schemaPattern, tablePattern, null)) {
                while (tables.next()) {

                    String tableCatalog = tables.getString(1);
                    String tableSchema = tables.getString(2);
                    String tableName = tables.getString(3);
                    String tableType = tables.getString(4);

                    if (tablePredicate.test(tableName)) {

                        jsonGenerator.writeStartObject();
                        jsonGenerator.writeStringField("type", "record");
                        jsonGenerator.writeStringField("namespace", avroNamespace);
                        jsonGenerator.writeStringField("name", tableNameToRecordName(tableName));
                        jsonGenerator.writeStringField("db-table", tableName);
                        jsonGenerator.writeArrayFieldStart("fields");

                        Set<String> primaryKeyColumns = new HashSet<>();
                        try (ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(tableCatalog, tableSchema, tableName)) {
                            while (primaryKeys.next()) {
                                primaryKeyColumns.add(primaryKeys.getString("COLUMN_NAME"));
                            }
                        }

                        try (ResultSet columns = databaseMetaData.getColumns(
                                tableCatalog, tableSchema, tableName, null)) {
                            while (columns.next()) {

                                String columnName = columns.getString("COLUMN_NAME");
                                int columnType = columns.getInt("DATA_TYPE");
                                String columnTypeName = columns.getString("TYPE_NAME");
                                int columnSize = columns.getInt("COLUMN_SIZE");
                                int decimalDigits = columns.getInt("DECIMAL_DIGITS");
                                boolean nullable = columns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;

                                jsonGenerator.writeStartObject();
                                jsonGenerator.writeStringField("name", columnNameToFieldName(columnName));
                                jsonGenerator.writeStringField("db-column", columnName);
                                if (primaryKeyColumns.contains(columnName)) {
                                    jsonGenerator.writeBooleanField("primaryKey", true);
                                }

                                AvroType avroType = columnTypeToAvroType(columnType, columnTypeName, columnSize, decimalDigits);
                                if (nullable) {
                                    jsonGenerator.writeArrayFieldStart("type");
                                } else {
                                    jsonGenerator.writeFieldName("type");
                                }

                                if (avroType.getLogicalType() == null) {
                                    jsonGenerator.writeString(avroType.getType());
                                } else {
                                    jsonGenerator.writeStartObject();
                                    jsonGenerator.writeStringField("type", avroType.getType());
                                    jsonGenerator.writeStringField("logicalType", avroType.getLogicalType());
                                    if (avroType == AvroType.DECIMAL) {
                                        jsonGenerator.writeNumberField("precision", columnSize);
                                        jsonGenerator.writeNumberField("scale", decimalDigits);
                                    }
                                    jsonGenerator.writeEndObject();
                                }

                                if (nullable) {
                                    jsonGenerator.writeString("null");
                                    jsonGenerator.writeEndArray();
                                }

                                jsonGenerator.writeEndObject();
                            }
                        }

                        jsonGenerator.writeEndArray();
                        jsonGenerator.writeEndObject();
                    }
                }
            }

        }

        jsonGenerator.writeEndArray();
        jsonGenerator.close();
    }

    private String tableNameToRecordName(String tableName) {
        return toUpperCamelCase(tableName);
    }

    private String columnNameToFieldName(String columnName) {
        return toLowerCamelCase(columnName);
    }

    private AvroType columnTypeToAvroType(int columnType, String columnTypeName, int columnSize, int decimalDigits) {
        return AvroType.getBySqlType(columnType, columnTypeName);
    }

    public void setTablePredicate(Predicate<String> tablePredicate) {
        this.tablePredicate = tablePredicate;
    }

}

```

## Serializing & De-serializing avro schema

```java
package com.abc.dali.avro;

import com.mercuria.dali.avro.conversion.InstantConversion;
import com.mercuria.dali.avro.conversion.LocalDateConversion;
import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AvroSerDe {

    private static final byte MAGIC_BYTE = 0x0;
    private static final int idSize = 4;

    private final SchemaRegistry schemaRegistry;
    private final boolean autoRegisterSchema;

    private final EncoderFactory encoderFactory = EncoderFactory.get();
    private final DecoderFactory decoderFactory = DecoderFactory.get();

    public AvroSerDe(SchemaRegistry schemaRegistry, boolean autoRegisterSchema) {
        this.schemaRegistry = schemaRegistry;
        this.autoRegisterSchema = autoRegisterSchema;
    }

    public byte[] serialize(String topic, GenericRecord value) throws IOException {
        if (value == null) {
            return null;
        }
        try {
            Schema schema = value.getSchema();
            int id;
            if (autoRegisterSchema) {
                id = schemaRegistry.register(topic, schema);
            } else {
                id = schemaRegistry.getId(topic, schema);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(MAGIC_BYTE);
            out.write(ByteBuffer.allocate(idSize).putInt(id).array());
            BinaryEncoder encoder = encoderFactory.directBinaryEncoder(out, null);
            GenericDatumWriter<Object> writer = new GenericDatumWriter<>(schema);
            writer.getData().addLogicalTypeConversion(new Conversions.DecimalConversion());
            writer.getData().addLogicalTypeConversion(new InstantConversion());
            writer.getData().addLogicalTypeConversion(new LocalDateConversion());
            writer.write(value, encoder);
            encoder.flush();
            byte[] bytes = out.toByteArray();
            out.close();
            return bytes;
        } catch (IOException | RuntimeException e) {
            throw new DaliAvroException("Error serializing Avro message", e);
        }
    }

    public GenericRecord deserialize(String topic, byte[] payload, Schema readerSchema) throws IOException {
        if (payload == null) {
            return null;
        }
        int id = -1;
        try {
            ByteBuffer buffer = getByteBuffer(payload);
            id = buffer.getInt();
            Schema schema = schemaRegistry.getBySubjectAndId(topic, id);
            int length = buffer.limit() - 1 - idSize;

            int start = buffer.position() + buffer.arrayOffset();
            GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema, readerSchema);
            reader.getData().addLogicalTypeConversion(new Conversions.DecimalConversion());
            reader.getData().addLogicalTypeConversion(new InstantConversion());
            reader.getData().addLogicalTypeConversion(new LocalDateConversion());
            return reader.read(null, decoderFactory.binaryDecoder(buffer.array(), start, length, null));

        } catch (IOException | RuntimeException e) {
            throw new DaliAvroException("Error deserializing Avro message for id " + id, e);
        }
    }

    private ByteBuffer getByteBuffer(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        if (buffer.get() != MAGIC_BYTE) {
            throw new DaliAvroException("Unknown magic byte!");
        }
        return buffer;
    }

}

```

## MsgBuilder
```java
package com.abc.dali.avro.replication;

import com.mercuria.dali.avro.DaliAvroException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecordBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mercuria.dali.avro.util.AvroSchemaUtil.getField;

public class ReplicationMessageBuilder {

    private final Schema schema;
    private final List<GenericData.Record> records = new ArrayList<>();

    public ReplicationMessageBuilder(Schema schema) {
        this.schema = schema;
    }

    public void addInsert(long sequence, Instant now, GenericData.Record dataRecord) {
        addOperation("INSERT", sequence, now, dataRecord);
    }

    public void addUpdate(long sequence, Instant now, GenericData.Record dataRecord) {
        addOperation("UPDATE", sequence, now, dataRecord);
    }

    public void addDelete(long sequence, Instant now, GenericData.Record dataRecord) {
        addOperation("DELETE", sequence, now, dataRecord);
    }

    private void addOperation(String operation, long sequence, Instant now, GenericData.Record dataRecord) {
        Schema dataSchema = dataRecord.getSchema();
        String dbTable = dataSchema.getProp("db-table");
        if (dbTable == null) {
            throw new DaliAvroException("Schema " + dataSchema.getFullName() + " does not contain a db-table property");
        }

        Schema replicationSchema = this.schema.getTypes().stream()
                .filter(s -> dbTable.equals(s.getProp("replicate-db-table")))
                .findFirst()
                .orElseThrow(() -> new DaliAvroException("No replication record for db-table '" + dbTable + "'"));

        Schema.Field headerField = getField(replicationSchema,"header");
        Schema headerSchema = headerField.schema();

        GenericRecordBuilder headerBuilder = new GenericRecordBuilder(headerSchema);
        Schema operationSchema = getField(headerSchema,"operation").schema();
        headerBuilder.set("operation", new GenericData.EnumSymbol(operationSchema, operation));
        headerBuilder.set("sequence", sequence);
        headerBuilder.set("timestamp", now);
        GenericData.Record headerRecord = headerBuilder.build();

        GenericRecordBuilder replicationBuilder = new GenericRecordBuilder(replicationSchema);
        replicationBuilder.set("header", headerRecord);
        replicationBuilder.set("data", dataRecord);

        records.add(replicationBuilder.build());
    }

    public GenericData.Record build() {
        Schema replicationBatch = this.schema.getTypes().stream()
                .filter(s -> s.getName().equals("ReplicationBatch"))
                .findFirst()
                .orElseThrow(() -> new DaliAvroException("No ReplicationBatch record type found for '" + schema.getName() + "'"));
        GenericRecordBuilder batchBuilder = new GenericRecordBuilder(replicationBatch);
        batchBuilder.set("Items", records);
        return batchBuilder.build();
    }

}

```
## ResultSet to Avro

```java
package com.abc.dali.avro.tools;

import com.mercuria.dali.avro.util.AvroType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecordBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.abc.dali.avro.util.AvroSchemaUtil.getFieldType;

public class ResultSet2AvroRecord {

    public static GenericData.Record buildRowRecord(Schema type, ResultSet rs) throws SQLException {
        GenericRecordBuilder builder = new GenericRecordBuilder(type);
        for (Schema.Field field : type.getFields()) {
            String dbColumn = field.getProp("db-column");
            if (dbColumn == null) {
                continue;
            }
            Object value = rs.getObject(dbColumn);
            Schema fieldSchema = getFieldType(field);
            AvroType avroType = AvroType.getByFieldSchema(fieldSchema);
            builder.set(field, avroType.convert(value));
        }
        return builder.build();
    }

}

```


