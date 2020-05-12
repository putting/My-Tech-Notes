# Uploading Excel sheets

## REST endpoint implemented by XlxToStorage class below, but also retrofit
Need to investigate the role of Dali in generating...
```java
import com.mercuria.dali.rest.Rest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.LocalDate;

@Rest
@Path("/api/upload")
public interface SpreadsheetUploadService {

    @POST
    void storeSpreadsheet(FileData file, String userName, String task, String page, LocalDate asOfDate, DataLoadMode loadMode);
}
```

```java
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableFileData.class)
@JsonDeserialize(as = ImmutableFileData.class)
public interface FileData {

    byte[] content();

}
```
and the actual service
```java
package com.mercuria.giant.spark.excel

import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.Optional

import com.mercuria.giant.spark.ExcelTaskColumns
import com.mercuria.giant.spark.store.StorageReadWrite
import com.mercuria.giant.spark.subledger.{DataLoadMode, FileData, SpreadsheetUploadService}
import org.apache.poi.ss.usermodel
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType.{BOOLEAN, NUMERIC, STRING}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DataType, DataTypes, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

class XlsxToStorage(spark: SparkSession, storageService: StorageReadWrite) extends SpreadsheetUploadService {

  val log: Logger = LoggerFactory.getLogger(classOf[XlsxToStorage])

  override def storeSpreadsheet(file: FileData, userName: String, task: String, page: String, asOfDate: LocalDate, loadMode: DataLoadMode): Unit = {

    log.debug("Parsing Spreadsheet at")

    val excelSheet: DataFrame = createDataFrameFromFile(file.content())

    val validatedSheet: DataFrame = validateColumnHeaders(excelSheet, task)

    log.debug("Successfully parsed spreadsheet. Storing in storageService")

    storeDataset(validatedSheet, task, userName, page, asOfDate, loadMode)
  }

  def validateColumnHeaders(dataFrame: DataFrame, task: String): DataFrame = {
    val columns: Seq[String] = dataFrame.columns.toSeq

    val expectedColumns: Seq[String] = new ExcelTaskColumns().getTaskColumns(task).asScala

    //This says that whitespace and casing should not matter. This converts all to uppercase and removes all whitespace before comparison. It is then renamed at the end of the class
    if (!columns.map(col => col.replace(" ", "").toUpperCase).map(col => expectedColumns.map(col => col.toUpperCase).contains(col)).foldLeft(true)(_ && _)) {
      throw new IllegalArgumentException(s"""Wrong format of excel sheet. Please enter in the agreed format. Agreed columns are ${expectedColumns.mkString(",")}""")
    }

    dataFrame
      .select(columns.map(col => col.toLowerCase).sorted.map(i => col(i)): _*)
      .toDF(expectedColumns.sorted: _*)
  }

  def createDataFrameFromFile(file: Array[Byte]): DataFrame = {
    val workbook: XSSFWorkbook = new XSSFWorkbook(new ByteArrayInputStream(file))
    try {

      val sheet = workbook.getSheetAt(0)

      val headerRow = sheet.getRow(0)
      val dataRow = sheet.getRow(1)

      val columnsTypes: Seq[StructField] = dataRow.cellIterator().asScala.map(data => {
        val headerTitle = getCellValue(headerRow.getCell(data.getColumnIndex)).toString
        val cellType = getCellType(data)
        StructField(headerTitle, cellType, nullable = false)
      }).toSeq

      val data = mapToRow(sheet.rowIterator().asScala.filter(row => row.getRowNum >= 1).toSeq).asJava

      spark.createDataFrame(data, StructType(columnsTypes))
    }
    catch {
      case e: Exception =>
        log.error("Could not create dataFrame from Spreadsheet. Please Check formatting ", e)
        throw e
    }
    finally {
      workbook.close()
    }

  }

  private def mapToRow(rows: Seq[usermodel.Row]): Seq[Row] = {
    //Builds an sql Row from cell values
    rows.map(r =>
      Row.fromSeq(
        r.asScala.toSeq
          .map(c => getCellValue(c))
      )
    )
  }

  private def getCellValue(cell: Cell): Any = {
    cell.getCellType match {
      case NUMERIC => cell.getNumericCellValue
      case STRING => cell.getStringCellValue
      case BOOLEAN => cell.getBooleanCellValue
      case _ => throw new Exception("Could not get cell value of cell. Column: " + cell.getColumnIndex + "Row: " + cell.getRowIndex)

    }
  }

  private def getCellType(cell: Cell): DataType = {
    cell.getCellType match {
      case NUMERIC => DataTypes.DoubleType
      case STRING => DataTypes.StringType
      case BOOLEAN => DataTypes.BooleanType
      case _ => throw new Exception("Could not get cell type of cell. Column: " + cell.getColumnIndex + "Row: " + cell.getRowIndex)

    }
  }

  private def storeDataset(excelSheet: DataFrame, taskName: String, userName: String, page: String, asOfDate: LocalDate, loadMode: DataLoadMode): Unit = {

    val task = loadMode.asTaskName(taskName)

    storageService.storeDataset(task, page, asOfDate, Optional.empty(), userName, excelSheet)
  }

}
```

and retrofit (Dali generated?)
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercuria.dali.rest.RetrofitHelper;
import com.mercuria.dali.rest.RetrofitWebServiceClientBuilder;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.function.Supplier;
import javax.annotation.Generated;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

@Generated(
    value = "com.mercuria.dali.annotationprocessors.RestClientGenerator",
    date = "2020-04-20T12:05:38.788+01:00[Europe/London]"
)
public class RetrofitSpreadsheetUploadServiceClient implements SpreadsheetUploadService {
  private static final Logger LOGGER = LoggerFactory.getLogger("com.mercuria.dali.rest.RetrofitSpreadsheetUploadServiceClient");

  private final RetrofitSpreadsheetUploadServiceClientApi clientApi;

  private final ObjectMapper objectMapper;

  private final int maxRetries;

  private RetrofitSpreadsheetUploadServiceClient(
      RetrofitSpreadsheetUploadServiceClientApi clientApi, ObjectMapper objectMapper,
      int maxRetries) {
    this.clientApi = clientApi;
    this.objectMapper = objectMapper;
    this.maxRetries = maxRetries;
  }

  public static final RetrofitWebServiceClientBuilder<RetrofitSpreadsheetUploadServiceClientApi, RetrofitSpreadsheetUploadServiceClient> createBuilder(
      String baseUrl) {
    return new RetrofitWebServiceClientBuilder<RetrofitSpreadsheetUploadServiceClientApi, RetrofitSpreadsheetUploadServiceClient>(baseUrl, LOGGER, RetrofitSpreadsheetUploadServiceClientApi.class, RetrofitSpreadsheetUploadServiceClient::new);
  }

  public static final RetrofitSpreadsheetUploadServiceClient create(String baseUrl) {
    return createBuilder(baseUrl).build();
  }

  @Override
  public void storeSpreadsheet(FileData file, String userName, String task, String page,
      LocalDate asOfDate, DataLoadMode loadMode) {
    Supplier<Call<ResponseBody>> responseSupplier = () -> this.clientApi.storeSpreadsheet(file, userName, task, page, asOfDate, loadMode);
    RetrofitHelper.getValue(LOGGER, responseSupplier, false, this.maxRetries);
  }
}
```
