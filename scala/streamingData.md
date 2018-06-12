# Streaming Maps of values

```scala
import java.io.OutputStream
import java.util.function.Consumer

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.mercuria.giant.spark.model._
import javax.ws.rs.core.StreamingOutput
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{DateType, StructField, StructType, TimestampType}
import org.apache.spark.sql.{DataFrame, Row}
import org.slf4j.Logger

trait DataFrameUtils {

  val log: Logger

  def stream(df: DataFrame, objectMapper: ObjectMapper, allEnumeratedValues: Map[String, Seq[String]] = Map.empty): StreamingOutput = {
    if (df == null) {
      new StreamingOutput() {
        override def write(output: OutputStream): Unit = objectMapper.writeValue(output, ImmutableDataFrameResult.builder().build())
      }
    } else {
      new StreamingOutput {
        override def write(output: OutputStream): Unit = {
          val jGenerator = objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false).getFactory.createGenerator(output, JsonEncoding.UTF8)
          jGenerator.writeStartObject()
          jGenerator.writeObjectField("schema", convertSchema(df.schema, allEnumeratedValues))

          jGenerator.writeArrayFieldStart("data")
          df.toJSON.repartition(math.max(4, (df.count() / 10000).toInt))
            .toLocalIterator()
            .forEachRemaining(new Consumer[String] {
              override def accept(row: String): Unit =
                jGenerator.writeRawValue(row)
            })
          jGenerator.writeEndArray()
          jGenerator.writeEndObject()
          jGenerator.flush()
        }
      }
    }
  }
```
