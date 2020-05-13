# Uploading binary files

Two approaches 
- With jersey and ws-rs
- Retrofit Generated (and Tag) and scala. Simple, but what does the gen retrofit do?
  Also thid eg converts to poi workbook

## WS-RS. MediaType.MULTIPART_FORM_DATA is important

@FormDataParam("file") InputStream inputStream, @FormDataParam("file") FormDataContentDisposition contentDisposition
are jersey annotations for the file.

```java
package com.mercuria.giant.server.resources;

import com.mercuria.giant.spark.subledger.DataLoadMode;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.time.LocalDate;

@Path("/upload")
@Api
public interface UploadService {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("uploadSpotPrices/{businessDate}/{loadMode}")
    void uploadManualSpotPrices(@Context SecurityContext securityContext, @PathParam("businessDate") LocalDate businessDate, @PathParam("loadMode") DataLoadMode loadMode, @FormDataParam("file") InputStream inputStream, @FormDataParam("file") FormDataContentDisposition contentDisposition);

    @GET
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Path("getTaskTemplate/{taskName}")
    Response getTaskTemplate(@Context SecurityContext securityContext, @PathParam("taskName") String taskName);

}
```

### Server-side Impl of REST resource
```java
    private void uploadSpreadsheet(SecurityContext securityContext, LocalDate businessDate, DataLoadMode loadMode, String task, String page, InputStream inputStream, FormDataContentDisposition contentDisposition) {
        if (contentDisposition.getFileName().contains(".xlsx")) {
            try {
                byte[] byteArray = IOUtils.toByteArray(inputStream);
                FileData fileData = ImmutableFileData.builder().content(byteArray).build();
                spreadsheetUploadResource.storeSpreadsheet(fileData, securityContext.getUserPrincipal().getName(), task, page, businessDate, loadMode);
            } catch (IOException e) {
                throw new BadRequestException("Could not read supplied spreadsheet. Please contact ITDEVFINANCE");
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        } else {
            throw new BadRequestException("File not xlsx format, please input xlsx");
        }
    }
```

### DevExtreme FileUploader handles client side

```js
<FileUploader
                readyToUploadMessage="Ready to upload"
                uploadedMessage="File Uploaded"
                uploadUrl={`${config.giantWebApi}/api/upload/uploadSpotPrices/${this.businessDate()}/${
                  this.props.match.params.mode
                }`}
                uploadMethod="POST"
                multiple={false}
                uploadMode="useButtons"
                allowedFileExtensions={['.xlsx']}
                name={'file'}
              />
```

## Retrofit

### The REST Service is simple, but generated RetrofitSpreadsheetUploadServiceClient does what?
Note the FileData which is a byte[] interface
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

### server-side spark impl
which takes byte stream and makes xl workbook new XSSFWorkbook(new ByteArrayInputStream(file)
```scala
  override def storeSpreadsheet(file: FileData, userName: String, task: String, page: String, asOfDate: LocalDate, loadMode: DataLoadMode): Unit = {

    log.debug("Parsing Spreadsheet at")

    val excelSheet: DataFrame = createDataFrameFromFile(file.content())

    val validatedSheet: DataFrame = validateColumnHeaders(excelSheet, task)

    log.debug("Successfully parsed spreadsheet. Storing in storageService")

    storeDataset(validatedSheet, task, userName, page, asOfDate, loadMode)
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
```

