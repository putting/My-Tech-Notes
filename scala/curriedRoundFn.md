# Curried so works in two scenarios

Initially it is passed into a map fn with the amount of decimal places. ie the map will return the number to format
Then lower down the decimal places and number are provided

```scala
  def round(dp: Int)(double: Double): Double = {
    BigDecimal(double)
      .setScale(dp, BigDecimal.RoundingMode.HALF_EVEN)
      .doubleValue()
  }
   .groupByKey(journal => {
        JournalEntry(
           amount = journal.headerAmount.map(round(2)).getOrElse(0.0),  
           ....
 })
      .mapValues(journal => {
        (
          LineItem(
            usdAmount = round(2)(journal.usdAmount),
            amountInTransactionCurrency = round(2)(journal.amountInTransactionCcy),          
           
  
```

