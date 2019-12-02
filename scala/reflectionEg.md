# Reflects on the Symbols (ie constants in an object)

Useful to get the list of columns for a dataframe
Can also be used to get all the val symbles in an object.
eg object abc {
val col1 = 'col1
val col2 = 'col2
}

```scala
  private def symbolValues[T: ClassTag](obj: T): Iterable[String] = {
    val rm = scala.reflect.runtime.currentMirror

    val symbol = rm.classSymbol(obj.getClass)
    val instanceMirror = rm.reflect(obj)

    symbol.toType.members.collect {
      case m: MethodSymbol if m.isGetter && m.isPublic && m.typeSignature.resultType =:= typeOf[scala.Symbol] =>
        val mirror: universe.MethodMirror = instanceMirror.reflectMethod(m)
        val value: scala.Symbol = mirror.apply().asInstanceOf[scala.Symbol]
        value.name
    }
  }
  
  
   @Test
  def checkJournalFieldsExist(): Unit = {
    val wantedFieldNames: Seq[String] = spark.fieldNamesOfCaseClass[Journal]

    val symbolFields = symbolValues(CommonFields) ++ symbolValues(InventoryFields) ++ symbolValues(VaultFields)

    val missingFields = wantedFieldNames.toSet.diff(symbolFields.toSet)

    Assert.assertTrue(s"should be no missing fields, but $missingFields were missing", missingFields.isEmpty)
  }
```
