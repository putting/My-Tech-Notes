# A filtering DSL (stored as json in db)

## TODO
This is currently a DSL. It will need to be converted into the native language to actually filter data. eg SQL or mongo expression.
It could conceivably be used to create java Predicate logic too


## Defined as a proto to capture Fluent DSL for boolean filtering logic
```
syntax = "proto3";

package mercuria.common;

option java_multiple_files = true;

// ExpressionDefinition is a message that represents an expression language that can be used in a variety of contexts.
// E.g. query builders, rule engines, access control, data validation, decision trees, etc.
// For example:
//{
//  expression {
//    logical_expr {
//      logical_expr {
//        operator: AND
//        operands {
//          comp_expr {
//            left_operand: trade_date
//            operator: LESS_THAN
//            right_operand {
//              value: "2024-03-01"
//            }
//          }
//        }
//        operands {
//          comp_expr {
//            left_operand: INSTRUMENT_TYPE
//            operator: IN
//            right_operand {
//              string_list {
//                  values: "APO"
//                  values: "EXCH_OPT"
//              }
//            }
//          }
//        }
//      }
//    }
//  }
//}
message ExpressionDefinition {
  oneof expression {
    LogicalExpression logical_expr = 1;
    ComparisonExpression comp_expr = 2;
  }
}

// A message that represents a logical expression of the sort (A AND B) OR (C AND D) OR E, etc.
message LogicalExpression {
  enum Operator {
    UNKNOWN = 0;
    AND = 1;
    OR = 2;
  }
  Operator operator = 1;
  repeated ExpressionDefinition operands = 2;
}

// A comparison expression is used to assert a condition on a field.
// For example: expiry_date < '2024-04-01', instrument_type IN ['APO','EXCH_OPT'], etc.
message ComparisonExpression {
  enum Operator {
    UNKNOWN = 0;
    LESS_THAN_OR_EQUAL = 1;
    GREATER_THAN_OR_EQUAL = 2;
    LESS_THAN = 3;
    GREATER_THAN = 4;
    NOT_EQUAL = 5;
    EQUAL = 6;
    IN = 7;
    NOT_IN = 8;
  }
  string left_operand = 1; // The left operand of the comparison expression, a field name. For example: instrument_type, trade_date, etc.
  Operator operator = 2; // The operator of the comparison expression: >, <, >=, <=, =, !=, IN, NOT_IN
  oneof right_operand {
    StringValues values = 3; // A list of string values. Used for IN and NOT_IN operators
    string value = 4; // A single string value. Used for the other operators
  }
}

message StringValues {
  repeated string values = 1;
}

```

### Json egs implemeting the above
```
{
  "name": "Filter for APOs and Vanillas",
  "definition": {
    "compExpr": {
      "leftOperand": "instrument_type",
      "operator": "IN",
      "values": {
        "values": [
          "APO",
          "EXCH_OPT"
        ]
      }
    }
  }
}
```
```
{
  "name": "Filter for ICTS trades in portfolios 123 and 456",
  "definition": {
    "logicalExpr": {
      "operator": "AND",
      "operands": [
        {
          "compExpr": {
            "leftOperand": "source_system",
            "operator": "EQUAL",
            "value": "ICTS"
          }
        },
        {
          "compExpr": {
            "leftOperand": "portfolio_id",
            "operator": "IN",
            "values": {
              "values": [
                "123",
                "456"
              ]
            }
          }
        }
      ]
    }
  }
}
```
```
{
  "name": "Filter for futures traded before 2021-01-01 or expiring before 2023-01-01",
  "definition": {
    "logicalExpr": {
      "operator": "AND",
      "operands": [
        {
          "logicalExpr": {
            "operator": "OR",
            "operands": [
              {
                "compExpr": {
                  "leftOperand": "trade_date",
                  "operator": "LESS_THAN_OR_EQUAL",
                  "value": "2021-01-01"
                }
              },
              {
                "compExpr": {
                  "leftOperand": "expiry_date",
                  "operator": "LESS_THAN_OR_EQUAL",
                  "value": "2023-01-01"
                }
              }
            ]
          }
        },
        {
          "compExpr": {
            "leftOperand": "instrument_type",
            "operator": "IN",
            "values": {
              "values": [
                "FUT"
              ]
            }
          }
        }
      ]
    }
  }
}
```

