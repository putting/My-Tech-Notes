# Purging Strategy illustrates of Sealed Trait Strategy Pattern

Purges data from a db using a number of stragies

## Sealed Trait Characteristics
The classes that implement the trait must be in the same file. This is a compact design.
The compiler also knows what all the possible values are and can check for that.

## Things of note
- Classes used for main impls
- Functional composition used for **combining strategies**. eg andThen, plus and UnionStrategy
- A single case class for data.
- A single object used as a static

```scala
sealed trait RetentionStrategy {
  def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo]

  def andThen(retentionStrategy: RetentionStrategy): RetentionStrategy = new RetainSequence(this, retentionStrategy)

  def plus(retentionStrategy: RetentionStrategy): RetentionStrategy = new UnionStrategy(this, retentionStrategy)

  def named(name: String): RetentionStrategy = new NamedRetentionStrategy(name, this)

  def name = toString

  protected def taskSet(storage: Seq[StorageInfo]): Seq[TaskSet] = {
    storage
      .groupBy(si => (si.task(), si.version()))
      .map(entry => {
        val date = entry._2.head.businessDate()
        val dateOption = if (date.isPresent) Some(date.get()) else None
        TaskSet(entry._1._1, entry._1._2, dateOption, entry._2)
      })
      .toSeq
  }
}

class NamedRetentionStrategy(name: String, retentionStrategy: RetentionStrategy) extends RetentionStrategy {
  override def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo] = retentionStrategy.retainFrom(storage)

  override def toString = name

}

class RetainVersionsToKeepPerDate(toKeep: Int) extends RetentionStrategy {

  override def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo] = {
    val taskSets: Seq[TaskSet] = taskSet(storage)

    val toRetain = taskSets
      .groupBy(ts => (ts.task, ts.businessDate))
      .mapValues(taskSets => {
        val results = taskSets
          .filter(_.allOkay)
          .sortBy(_.version)
          .reverse

        results
          .take(toKeep)
      })
      .values
      .flatten
      .flatMap(_.pages)
      .toSeq

    toRetain

  }

  override def toString = s"Retain $toKeep Versions"
}

// purge anything in a failed state, or where the state is null and it is aged over 1 day
// 1 day is massive overkill, but anything older than 1 day and null status, we can assume is failed
object PurgeFailed extends RetentionStrategy {
  override def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo] = {
    storage
      .filterNot(si => {
        if (si.optionalState().isPresent) {
          si.state() == StorageInfo.State.FAIL
        } else {
          si.creationDate().isBefore(Instant.now().minus(1, ChronoUnit.DAYS))
        }

      })
  }
}

class RetainDuration(period: Period) extends RetentionStrategy {
  override def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo] = {

    val cutOffPoint: LocalDate =
      LocalDate.now()
        .minus(period)
        .minusDays(1)

    val taskSets: Seq[TaskSet] = taskSet(storage)

    taskSets
      .filter(ts => ts.businessDate match {
        case None => true
        case Some(date) => date.isAfter(cutOffPoint)
      })
      .flatMap(_.pages)
  }

  override def toString = s"Retain $period Versions"
}

class SplitStrategy(filterFn: StorageInfo => Boolean, ifTrueStrategy: RetentionStrategy, ifFalseStrategy: RetentionStrategy) extends RetentionStrategy {
  override def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo] = {
    val (whenTrue, whenFalse) = storage.partition(filterFn)
    ifTrueStrategy.retainFrom(whenTrue) ++ ifFalseStrategy.retainFrom(whenFalse)
  }
}

class RetainPosted(postingStatus: Seq[PostingStatus]) extends RetentionStrategy {

  override def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo] = {

    val taskSets: Seq[TaskSet] = taskSet(storage)

    val posted = postingStatus
      .map(pd => TaskKey(pd.task(), pd.version()))
      .toSet

    taskSets.filter(ts => posted.contains(ts.taskKey))
      .flatMap(_.pages)
  }

  override def toString = s"Retain Posted"

}

class UnionStrategy(rs1: RetentionStrategy, rs2: RetentionStrategy) extends RetentionStrategy {
  override def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo] = rs1.retainFrom(storage) ++ rs2.retainFrom(storage)

  override def toString = s"Retain ($rs1) and ($rs2)"
}

class RetainSequence(rs1: RetentionStrategy, rs2: RetentionStrategy) extends RetentionStrategy {
  override def retainFrom(storage: Seq[StorageInfo]): Seq[StorageInfo] = {
    val retainedFromStrategy1: Seq[StorageInfo] = rs1.retainFrom(storage)
    val retainedFromStrategy1And2: Seq[StorageInfo] = rs2.retainFrom(retainedFromStrategy1)
    retainedFromStrategy1And2
  }

  override def toString = s"Retain ($rs1) and then ($rs2)"
}

case class TaskSet(task: String, version: Long, businessDate: Option[LocalDate], pages: Seq[StorageInfo]) {
  def allOkay: Boolean = pages.forall(p => {
    val state: State = p.optionalState().orElse(State.NOT_FOUND)
    val okayState = state == State.OK || state == State.STALE
    //if not present it might be because it is currently being loaded, we have a purge all failed to catch actual fails.
    okayState || !p.optionalState().isPresent
  })

  def taskKey: TaskKey = TaskKey(task, version)
}

```
