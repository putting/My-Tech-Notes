# Purging

## Commentary
Purges based on a number of strategies, which can be composed in different ways
- RetainDuration, RetainPosted, RetainSequence
- SplitStrategy, UnionStrategy

I guess the reson why the retain strategy and the composing elements are also strategy is to allow the building/composing.

```scala
class StoragePurge(storageInfos: Seq[StorageInfo], storageInfoLinks: Seq[StorageInfoLink], postingStatus: Seq[PostingStatus], auditDates: Set[LocalDate]) {

  private val RetainFiveYears = new RetainDuration(Period.ofYears(5))
  private val Retain3Versions = new RetainVersionsToKeepPerDate(3)
  private val Retain1Versions = new RetainVersionsToKeepPerDate(1)
  private val RetainPosted = new RetainPosted(postingStatus)
  private val RetainThreeMonths = new RetainDuration(Period.ofMonths(3))

  private val SubledgerMonthEnd = (RetainFiveYears andThen Retain3Versions) plus RetainPosted
  private val SubledgerDaily = (RetainThreeMonths andThen Retain3Versions) plus RetainPosted
  private val WIP = RetainThreeMonths andThen Retain1Versions

  private val MonthEnd = RetainFiveYears andThen Retain3Versions
  private val Daily = RetainThreeMonths andThen Retain3Versions

  private val SubledgerLeaves = Seq(
    JournaliserKeys.Accounting, JournaliserKeys.AggregatedInventoryAccounting, JournaliserKeys.AggregatedInventoryEconomic,
    JournaliserKeys.InventoryPosition, JournaliserKeys.InventoryBuildDrawsToStockReportReconciliation, JournaliserKeys.InventoryVolumetricReconciliation,
    //note cost linkage is not used unless late allocations, but we could do with keeping this around
    RawDataKeys.costLinkage
  )
  private val EndurDerivativesLeaves = Seq(JournaliserKeys.EndurClearedDerivatives, JournaliserKeys.EndurToCubeRec, "cleared_derivatives")
  private val DataControl: String = ".*Check"
  private val DateRX = """20\d\d-[01]\d-[0123]\d"""

  private val IsAuditDate: StorageInfo => Boolean = si => auditDates.contains(si.businessDate().orElse(LocalDate.MAX))

  private val purgeStrategies: Seq[StrategyMapping] =
    Seq(
      StrategyMapping(ModeTaskStorageMatcher(DataLoadMode.AUDIT, SubledgerLeaves), SubledgerMonthEnd named "SL ICTS Audit Month End"),
      StrategyMapping(ModeTaskStorageMatcher(DataLoadMode.DAILY, SubledgerLeaves), SubledgerDaily named "SL ICTS Live Daily"),
      StrategyMapping(ModeTaskStorageMatcher(DataLoadMode.VAULT_DAILY, SubledgerLeaves), SubledgerDaily named "SL Vault Live Daily"),
      StrategyMapping(ModeTaskStorageMatcher(DataLoadMode.VAULT_AUDIT, SubledgerLeaves), SubledgerDaily named "SL Vault Audit Daily"),
      StrategyMapping(ModeTaskStorageMatcher(DataLoadMode.VAULT_EOD, SubledgerLeaves), new SplitStrategy(IsAuditDate, SubledgerMonthEnd, SubledgerDaily) named "SL Vault EOD"),
      // all Endur derivatives run as vault_daily
      StrategyMapping(ModeTaskStorageMatcher(DataLoadMode.VAULT_DAILY, EndurDerivativesLeaves), new SplitStrategy(IsAuditDate, SubledgerMonthEnd, SubledgerDaily) named "Endur Derivatives"),

      StrategyMapping(ModeRegexTaskStorageMatcher(DataLoadMode.AUDIT, DataControl), MonthEnd named "Control ICTS Audit Month End"),
      StrategyMapping(ModeRegexTaskStorageMatcher(DataLoadMode.DAILY, DataControl), Daily named "Control ICTS Live Daily"),
      StrategyMapping(ModeRegexTaskStorageMatcher(DataLoadMode.VAULT_DAILY, DataControl), Daily named "Control Vault Daily"),
      StrategyMapping(ModeRegexTaskStorageMatcher(DataLoadMode.VAULT_AUDIT, DataControl), Daily named "Control Vault Audit"),

      // We should come up with a more complete retention strategy for these recs.
      // Latest 3 is useful mainly for development.
      StrategyMapping(RegexTaskStorageMatcher(".*_pl_rec_.*"), Retain3Versions named "P&L Rec 3 latest versions"),
      StrategyMapping(RegexTaskStorageMatcher(".*_sap_voucher_.*"), Retain3Versions named "SAP Voucher Rec 3 latest versions"),
      StrategyMapping(RegexTaskStorageMatcher(".*_subledger_to_sap_rec"), Retain3Versions named "SAP Giant Rec 3 latest versions"),

      StrategyMapping(RegexTaskStorageMatcher("vault_rec_.*"), Daily named "Vault Rec Daily"),
      StrategyMapping(RegexTaskStorageMatcher("compare_.*"), Daily named "Comparison Rec Daily"),
      StrategyMapping(RegexTaskStorageMatcher(".*_wip"), WIP named "WIP datasets"),


      StrategyMapping(TaskStorageMatcher(Set("SubledgerITData")), Retain3Versions named "Regression results"),

      //for every raw data, let keep the latest one version, in-case we need it later.
      StrategyMapping(TaskStorageMatcher(storageInfos.filter(si => si.page() == RawDataKeys.Pages.RawData).map(_.task()).toSet), Retain1Versions)

    )
      //for every strategy, first purge failed
      .map(mapping => mapping.copy(retentionStrategy = (PurgeFailed andThen mapping.retentionStrategy) named mapping.retentionStrategy.name))

  def findStorageToPurge(): Result = {

    //key = to task
    //value = from task
    val linksToTarget: Map[TaskKey, Set[TaskKey]] = storageInfoLinks
      .toSet
      .groupBy(TaskKey.applyTo)
      .mapValues(sils => sils.map(TaskKey.applyFrom))

    //this recursively looks for links to the given task key, so will return everything which is an input to the given task kep
    def findDependants(taskKey: TaskKey): Set[TaskKey] = {
      linksToTarget
        .get(taskKey)
        .map(directInputs => {
          val keys: Set[TaskKey] = directInputs.flatMap(input => findDependants(input))
          directInputs ++ keys
        }).getOrElse(Set())
    }

    val storageInfoByTaskType: Map[TaskType, Seq[StorageInfo]] = storageInfos.groupBy(TaskType.apply)

    val storageInfoByTaskKey: Map[TaskKey, Seq[StorageInfo]] = storageInfos.groupBy(TaskKey.apply)

    val purgeResults: Set[PurgeResult] = storageInfoByTaskType
      .flatMap {
        case (taskType, storageInfosByType) => {
          val matchingStrategies = purgeStrategies
            //find all purge strategies that match
            .filter(mapping => mapping.storageMatcher.isMatch(taskType))
            .map(_.retentionStrategy)

          if (matchingStrategies.isEmpty) {
            None
          } else {
            //combine all matching strategies into one
            val retentionStrategy = matchingStrategies.reduce((s1, s2) => s1.plus(s2))

            //apply found retention strategy on given storage info
            val retainedStorageInfo: Seq[StorageInfo] = retentionStrategy.retainFrom(storageInfosByType)
            val retainedTaskKeys: Seq[TaskKey] = retainedStorageInfo.map(TaskKey(_))

            //ergo purged is what is not retained
            val purgedStorageInfo: Seq[StorageInfo] = (storageInfosByType.toSet -- retainedStorageInfo.toSet).toSeq
            val purgedTaskKeys: Seq[TaskKey] = purgedStorageInfo.map(TaskKey(_))

            //for each task key, find all of its dependants
            val retainedDependants: Seq[TaskKey] = retainedTaskKeys.flatMap(findDependants)
            val purgedDependants: Seq[TaskKey] = purgedTaskKeys.flatMap(findDependants)

            //total retained is those directly retained + their dependants
            val allRetained = (retainedTaskKeys ++ retainedDependants).toSet
            //note, something may be listed as purged and retained, and retained overrules purged
            val allPurged = (purgedTaskKeys ++ purgedDependants).toSet -- allRetained
            Some(PurgeResult(taskType, retentionStrategy, allRetained, allPurged))
          }
        }
      }.toSet


    val (mappedRetainedInfo, mappedPurgedInfo) = purgeResults.foldLeft((Set[StorageInfo](), Set[StorageInfo]())) {
      case ((retained: Set[StorageInfo], purged: Set[StorageInfo]), purgeResult: PurgeResult) =>
        val purgedTables: Set[StorageInfo] = purgeResult.purged.flatMap(tk => storageInfoByTaskKey.getOrElse(tk, Set()))
        val retainedTables: Set[StorageInfo] = purgeResult.retained.flatMap(tk => storageInfoByTaskKey.getOrElse(tk, Set()))
        (retained ++ retainedTables, purged ++ purgedTables)
    }

    // so most tasks will not be directly mapped with a retention strategy.
    // however most should be indirectly mapped via a parent.

    // we do *not* want to purge anything neither directly or indirectly mapped.
    // i.e. if a new task type comes along, we dont want to do any purge until it becomes mapped (directly or indirectly).

    // the idea here is to group by the task type of anything retained/purged (directly or indirectly)
    // anything not grouped means there is is no strategy defined, so we should not purge
    // if it is grouped then we can look at what is retained and purged based on a strategy
    val mappedTaskType: Set[TaskType] = purgeResults
      .flatMap(rs => {
        val retained = rs.retained.map(rpk => rpk.taskType)
        val purged = rs.purged.map(rpk => rpk.taskType)
        retained ++ purged
      })

    val unmappedTaskTypes: Set[TaskType] = storageInfoByTaskType.keySet -- mappedTaskType
    val unmappedTaskKeys: Set[TaskKey] = storageInfoByTaskType
      .filterKeys(unmappedTaskTypes.contains)
      .valuesIterator
      .flatten
      .map(TaskKey.apply)
      .toSet

    // we need to compute a 'do not purge' purge result for all unmapped task keys and their inputs.
    // note, ideally we won't have unmapped task types, however from time to time we will add new pages
    val unMappedRetainInfo: Set[StorageInfo] = unmappedTaskKeys
      .flatMap(utk => findDependants(utk))
      .flatMap(tk => storageInfoByTaskKey.getOrElse(tk, Seq()))

    //just in-case there is any overlap
    val toPurge: Set[StorageInfo] = {
      //just check the same table isn't in both storage info
      val tablesToSave: Set[String] = mappedRetainedInfo.map(_.tablename()) ++ unMappedRetainInfo.map(_.tablename())
      val toPurge = mappedPurgedInfo.filterNot(p => tablesToSave.contains(p.tablename()))
      toPurge
    }

    Result(toPurge, unmappedTaskTypes)
  }

  case class StrategyMapping(storageMatcher: StorageMatcher, retentionStrategy: RetentionStrategy)

  case class PurgeResult(taskType: TaskType, retentionStrategy: RetentionStrategy, retained: Set[TaskKey], purged: Set[TaskKey])

  sealed trait StorageMatcher {
    def isMatch(taskType: TaskType): Boolean
  }

  object ModeTaskStorageMatcher {
    def apply(dataLoadMode: DataLoadMode, task: String): ModeTaskStorageMatcher = ModeTaskStorageMatcher(dataLoadMode, Seq(task))
  }

  case class ModeTaskStorageMatcher(dataLoadMode: DataLoadMode, tasks: Seq[String]) extends StorageMatcher {
    override def isMatch(taskType: TaskType): Boolean = tasks.exists(task => {
      val loadModeTaskName: String = dataLoadMode.asTaskName(task).toPath
      loadModeTaskName == taskType.task ||
        (taskType.task.startsWith(loadModeTaskName) && taskType.task.matches(s"${loadModeTaskName}_$DateRX"))
    })
  }

  case class ModeRegexTaskStorageMatcher(dataLoadMode: DataLoadMode, regex: String) extends StorageMatcher {
    override def isMatch(taskType: TaskType): Boolean = dataLoadMode.matchesLoadMode(taskType.task) &&
      DataLoadMode.removeLoadModeFromTask(taskType.task).matches(regex)
  }

  case class RegexTaskStorageMatcher(regex: String) extends StorageMatcher {
    override def isMatch(taskType: TaskType): Boolean = taskType.task.matches(regex)
  }

  case class TaskStorageMatcher(tasks: Set[String]) extends StorageMatcher {
    override def isMatch(taskType: TaskType): Boolean = tasks.contains(taskType.task)
  }


}
```
