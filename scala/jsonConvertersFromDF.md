# Json Convertes using spark

## Overview
Rules re stored in db, with conditoins as json.


## The rules are stored in the db in json.

rule_state:
{"costTypeCode":[],"commodity":[],"counterparty":[20493],"portfolio":[],"bookingCompany":[],"profitCenter":[]}
applies_state:
{"post":true,"calculation":true,"recon":[],"control":["Internal Trade Check"]}

## Why?
- Complex combination of rules
- The json can be deserialised straight to the UI

eg. 

```scala
  def getComplexExclusionRules(spark: SparkSession): Dataset[SparkExclusionRule] = {
    import com.mercuria.giant.spark.pipeline.Implicits._
    import org.apache.spark.sql.Encoders
    import spark.implicits._
    complexExclusionRules(spark)
      .withColumn("appliesState", functions.from_json($"appliesState", Encoders.product[AppliesState].schema))
      .withColumn("ruleState", functions.from_json($"ruleState", Encoders.product[RuleState].schema))
      .asWithCast[SparkExclusionRule]
  }
```

## Flat mapping json rules to a DataFrame. This is the step
```scala
override def apply(data: DataFrame): DataFrame = {
    val wantedRules = rules
      .collect()
      .filter(cer =>
        ComplexDataType.valueOf(cer.dataType) == ComplexDataType.ALL || ComplexDataType.valueOf(cer.dataType) == complexDataType
      )
      //if running for endur, we dont want any user defined rules
      .filter(_ => !complexDataType.isEndur)

    val appliedUserManagedRules = wantedRules
      .foldLeft(data) {
        case (df, rule) =>
          val condition: String = rule.ruleState.createFilterCondition(complexDataType)
          if (StringUtils.isBlank(condition)) {
            df
          } else {
            logger.debug(s"Applying rule ${rule.name} - ${rule.description} - $condition")
            df
              .withExclusionIf(functions.expr(condition), rule.appliesState.ruleAction, s"Excluded due to ${rule.name}")
          }
      }

    val itMangedRulesToApply = IT_MANAGED_RULES
      .filter(er => er.dataType == complexDataType)

    val appliedAllRules = itMangedRulesToApply
      .foldLeft(appliedUserManagedRules) {
        case (df, rule) =>
          logger.debug(s"Applying rule $rule")
          //NTH: Would be fab if the exclusion reason wasn't just a static string.
          df.withExclusionIf(rule.sql, rule.ruleAction, rule.explanation)
      }

    appliedAllRules
  }
```

## The rules are mapped to a string using this Some/None technique to AND

```scala
private[spark] case class RuleState(bookingCompany: List[Long], profitCenter: List[String], portfolio: List[Long], counterparty: List[String], commodity: List[String], costTypeCode: List[String]) {
  def createFilterCondition(complexDataType: ComplexDataType): String = {
    List(
      if (bookingCompany.nonEmpty) Some(s"${CommonFields.bookCompVCI.name} in ${bookingCompany.mkString("(", ",", ")")}") else None,
      if (profitCenter.nonEmpty) Some(s"${CommonFields.profitCenter.name} in ${profitCenter.mkString("('", "','", "')")}") else None,
      if (portfolio.nonEmpty) Some(s"${CommonFields.portfolio.name} in ${portfolio.mkString("(", ",", ")")}") else None,
      if (counterparty.nonEmpty)
        if (complexDataType == ComplexDataType.ICTS_COST)
          Some(s"${CommonFields.ctpyVCI.name} in ${counterparty.mkString("('", "','", "')")}")
        else
          None
      else
        None,
      if (commodity.nonEmpty)
        if (complexDataType == ComplexDataType.ICTS_COST)
          Some(s"${CostFields.costCode.name} in ${commodity.mkString("('", "','", "')")}")
        else
          Some(s"${CommonFields.commodity.name} in ${commodity.mkString("('", "','", "')")}")
      else
        None,
      if (costTypeCode.nonEmpty) Some(s"${CostFields.costTypeCode.name} in ${costTypeCode.mkString("('", "','", "')")}") else None
    )
      .flatten
      .mkString(" AND ")
  }
}
```
