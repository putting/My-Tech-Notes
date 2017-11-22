# Implicits
From what I remember a powerful matching

## Implicit Class
Should be declared within another object, trait, class.

The example below indicates how you can augment an existing class in this case a DataSet, so it has additional methods like withErrorIf.
The Implicits object will need to be imported into any class you plan to use this..

```scala
object Implicits {
.....
  implicit class SymbolAwareDataFrame(val dataset: Dataset[_]) extends AnyVal {
  def withErrorIf(condition: Column, message: String): DataFrame = withErrorIf(condition, lit(message))
   private def withArrayColIf(column: String, condition: Column, message: Column): DataFrame = {
      val datasetToUpdate: DataFrame =
        if (dataset.columns.contains(column)) {
          dataset.toDF()
        } else {
          dataset.withColumn(column, array())
        }
      datasetToUpdate.withColumn(column,
        when(condition, updateArray(col(column), message))
          otherwise col(column)
      )
    }
  ...
  }
```  

## Implicit conversions
An example where doing java convertions (behind the scenes wherever the class is imported)
Spark comes with similar conversion from Struct Field to Strings, Double etc...

eg. from spark DateType uses .as[Type] built into spark lib
data.select(col).distinct().as[java.sql.Date].collect().map(_.toLocalDate).toSet

```scala
private object JavaConvertImplicits {
  implicit def jOptionalToScalaOption[A, B](optional: Optional[A])(implicit ev$1: A => B): Option[B] = if (optional.isPresent) Some(optional.get()) else None

  implicit def jLocalDateToSqlDate(localDate: LocalDate): java.sql.Date = java.sql.Date.valueOf(localDate)

  implicit def jOptionalIntegerToScalaOption(optional: Optional[java.lang.Integer]): Option[Int] = if (optional.isPresent) Some(optional.get()) else None

  implicit def jOptionalLocalDateToScalaOptionSqlDaTE(optional: Optional[java.time.LocalDate]): Option[java.sql.Date] = if (optional.isPresent) Some(optional.get()) else None

}
```
