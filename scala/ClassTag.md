# ClassTag 

 A ClassTag[T] stores the **erased class** of a given type `T`, accessible via the `runtimeClass`
 * field. This is particularly useful for instantiating `Array`s whose element types are unknown
 * at compile time.
 *
 * `ClassTag`s are a weaker special case of [[scala.reflect.api.TypeTags#TypeTag]]s, in that they
 * wrap only the runtime class of a given type, whereas a `TypeTag` contains all static type
 * information.
 
 Used in Implicits to help with the Spark array processing: equals, intersect, which was a bit complicated.
 ie Arrays != and sets too
 ```scala
   implicit class ExtraColumnMethods(val column: Column) extends AnyVal {
    def collectUnique(separator: String): Column = array_join(array_sort(array_distinct(column)), separator)

    def equalsSet[T: ClassTag](set: Set[T]): Column = array_sort(array_distinct(column)) === array_sort(lit(set.toArray))

    def intersectSet[T: ClassTag](set: Set[T]): Column = arrays_overlap(column, lit(set.toArray))
  }

  implicit class ExtraColumnSymbolMethods(val column: Symbol) extends AnyVal {
    def collectUnique(separator: String): Column = col(column.name).collectUnique(separator)

    def equalsSet[T: ClassTag](set: Set[T]): Column = col(column.name).equalsSet(set)

    def intersectSet[T: ClassTag](set: Set[T]): Column = col(column.name).intersectSet(set)
  }
 ```
