# Equivalent of the varargs array or rest

def fromColumnName(columnName: String): ExclusionColumn = {
```
val Array(_, order, ruleAction@_*) = columnName.split(Separator)
      ExclusionColumn(ruleAction.map(ro => RuleAction.values()(ro.toInt)), order.toInt)
    }
```
