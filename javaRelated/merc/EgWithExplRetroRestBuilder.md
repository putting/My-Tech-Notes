# Eg using the @REst tag rather than @RestClient

Rememner there is an imp thats coded as always and the client just delegates 

## Service interface using jaw-rs annos

```java
import com.mercuria.dali.rest.Rest;
import com.mercuria.giant.database.schema.posting.PostingDetail;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.LocalDate;

@Rest
@Path("/api/spark/subledger")
public interface JournalToSapService {

    @POST
    PostingDetail promoteJournalsToSapForEntity(int subledger, LocalDate businessDate, String entity);

}
```

## The client api

```java
import com.mercuria.dali.rest.ResponseWrapper;
import com.mercuria.giant.database.schema.posting.PostingDetail;
import java.lang.String;
import java.time.LocalDate;
import javax.annotation.Generated;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

/**
 * This interface is generated for Retrofit to use. */
@Generated(
    value = "com.mercuria.dali.annotationprocessors.RestClientGenerator",
    date = "2020-04-27T15:55:02.851+01:00[Europe/London]"
)
interface RetrofitJournalToSapServiceClientApi {
  @POST("/api/spark/subledger/promoteJournalsToSapForEntity/{subledger}/{businessDate}/{entity}")
  @Streaming
  Call<ResponseWrapper<PostingDetail>> promoteJournalsToSapForEntity(
      @Path("subledger") int subledger, @Path("businessDate") LocalDate businessDate,
      @Path("entity") String entity);
}

```

## The Client

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercuria.dali.rest.ResponseWrapper;
import com.mercuria.dali.rest.RetrofitHelper;
import com.mercuria.dali.rest.RetrofitWebServiceClientBuilder;
import com.mercuria.giant.database.schema.posting.PostingDetail;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.function.Supplier;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

@Generated(
    value = "com.mercuria.dali.annotationprocessors.RestClientGenerator",
    date = "2020-04-27T15:55:02.851+01:00[Europe/London]"
)
public class RetrofitJournalToSapServiceClient implements JournalToSapService {
  private static final Logger LOGGER = LoggerFactory.getLogger("com.mercuria.dali.rest.RetrofitJournalToSapServiceClient");

  private final RetrofitJournalToSapServiceClientApi clientApi;

  private final ObjectMapper objectMapper;

  private final int maxRetries;

  private RetrofitJournalToSapServiceClient(RetrofitJournalToSapServiceClientApi clientApi,
      ObjectMapper objectMapper, int maxRetries) {
    this.clientApi = clientApi;
    this.objectMapper = objectMapper;
    this.maxRetries = maxRetries;
  }

  public static final RetrofitWebServiceClientBuilder<RetrofitJournalToSapServiceClientApi, RetrofitJournalToSapServiceClient> createBuilder(
      String baseUrl) {
    return new RetrofitWebServiceClientBuilder<RetrofitJournalToSapServiceClientApi, RetrofitJournalToSapServiceClient>(baseUrl, LOGGER, RetrofitJournalToSapServiceClientApi.class, RetrofitJournalToSapServiceClient::new);
  }

  public static final RetrofitJournalToSapServiceClient create(String baseUrl) {
    return createBuilder(baseUrl).build();
  }

  @Override
  public PostingDetail promoteJournalsToSapForEntity(int subledger, LocalDate businessDate,
      String entity) {
    Supplier<Call<ResponseWrapper<PostingDetail>>> responseSupplier = () -> this.clientApi.promoteJournalsToSapForEntity(subledger, businessDate, entity);
    return RetrofitHelper.getValue(LOGGER, responseSupplier, false, this.maxRetries).getBody();
  }
}
```
