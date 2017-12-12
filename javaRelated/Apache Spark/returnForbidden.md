# Forbidden Exception

When legal entity not found

```scala
import java.time.LocalDate
import javax.ws.rs.ForbiddenException
import javax.ws.rs.core.Response

import com.google.common.collect.ImmutableList
import com.mercuria.giant.spark.model.{CombinatorRule, DataFrameResult}
import com.mercuria.giant.spark.store._
import com.mercuria.giant.spark.subledger.common.CommonFields.legalEntity
import com.mercuria.giant.spark.{DataFrameExcelUtils, DataFrameUtils}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.{immutable, mutable}

class SparkStorageService(storage: Storage) extends StorageService with DataFrameUtils with DataFrameExcelUtils {

  val log: Logger = LoggerFactory.getLogger(classOf[SparkStorageService])

  import collection.JavaConversions._
  import collection.JavaConverters._

  override def listDatasets(businessDate: LocalDate): ImmutableList[TaskStorage] = {
    val grouped: Map[TaskStorage, mutable.Buffer[(TaskStorage, TaskStorageDetail)]] =
      storage.listDatasets
        .filter(p = (ds: StorageInfo) => !ds.businessDate().isPresent || ds.businessDate().get() == businessDate)
        .filter(p = (ds: StorageInfo) => ds.state().isPresent && ds.state().get() == StorageInfo.State.OK)
        .map(si => (ImmutableTaskStorage.builder.task(si.task).businessDate(si.businessDate).build.asInstanceOf[TaskStorage], ImmutableTaskStorageDetail.builder.page(si.page).creationDate(si.creationDate).username(si.username).version(si.version).build.asInstanceOf[TaskStorageDetail]))
        .groupBy(_._1)

    val d: immutable.Iterable[TaskStorage] = grouped
      .map { case (ts, tsdl) => ImmutableTaskStorage.builder.from(ts).detail(tsdl.map(_._2).sortBy(_.version())).build }

    ImmutableList.copyOf(d.asJava)
  }

  override def loadDataset(task: String, page: String, businessDate: LocalDate, version: Int, legalEntities: java.util.Set[String], combinatorRule: CombinatorRule): DataFrameResult =
    convert(loadAndFilterDataframe(task, page, businessDate, version, legalEntities.asScala, combinatorRule))

  override def exportDataset(task: String, page: String, businessDate: LocalDate, version: Int, legalEntities: java.util.Set[String], combinatorRule: CombinatorRule): Response = {
    val workbook = buildWorkbook(loadAndFilterDataframe(task, page, businessDate, version, legalEntities.asScala, combinatorRule), s"$task-$page-v$version")
    Response.ok(streamTheData(workbook)).build()
  }

  private def loadAndFilterDataframe(task: String, page: String, businessDate: LocalDate, version: Int, legalEntities: Iterable[String], combinatorRule: CombinatorRule) = {
    val dataFrame: DataFrame = storage.fetchSpecificDataPage(task, page, version, businessDate)
    val filteredDataFrame = filter(dataFrame, combinatorRule)
    legalEntities.toList match {
      //user is allowed access to all legal entities
      case "*" :: Nil => filteredDataFrame
      //invalid usecase - api was called without passing in legal entities
      case Nil => dataFrame.limit(0)
      //user has specific permissions
      case list => {
        if (filteredDataFrame.columns.contains(legalEntity.name)) {
          log.info("Filtering result set '{}' '{}' {} v{} by legal entities {}", task, page, businessDate, version.asInstanceOf[AnyRef], list)
          filteredDataFrame.filter(col(legalEntity.name).isin(list: _*))
        }
        else {
          //cannot determine legal entity column to filter on, so don't return any data.
          throw new ForbiddenException("Cannot determine legal entity column to filter on, and you do not have blanket access")
        }
      }
    }
  }
}

```
