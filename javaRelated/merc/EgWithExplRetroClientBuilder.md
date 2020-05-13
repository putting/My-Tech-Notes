# Example of how the client builder works

# Questions for Richard on Dali, REst, Retrofit

## Define a service interface
So we define a java interface using javax.ws.rs annotations.
The @RestClient tag is for the Dali RestClientGenerator to create a ** Retrofit client ** from.

```java
@Produces(MediaType.APPLICATION_JSON)
@RestClient
@Path("/api")
public interface EntitlementsService {

    @GET
    @Path("/reporting/entitlement/domain/{domain}")
    Set<WorkflowEntitlement> getRoleResourceActionMapping(@PathParam("domain") String domain);
```	

## Generated client api is: 
Now using the Retrofit annotations.

```java
import javax.annotation.Generated;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

/**
 * This interface is generated for Retrofit to use. */
@Generated(
    value = "com.mercuria.dali.annotationprocessors.RestClientGenerator",
    date = "2020-04-27T18:09:05.189+01:00[Europe/London]"
)
interface RetrofitEntitlementsServiceClientApi {
  @GET("/api/reporting/entitlement/domain/{domain}")
  @Streaming
  Call<Set<WorkflowEntitlement>> getRoleResourceActionMapping(@Path("domain") String domain);
```

## Final Generated Retrofit Client using the RetrofitWebServiceClientBuilder to transform api into client. 
Which is the main Retrofit.build() process

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercuria.dali.rest.RetrofitHelper;
import com.mercuria.dali.rest.RetrofitWebServiceClientBuilder;
import com.mercuria.giant.workflow.model.User;
import com.mercuria.giant.workflow.model.WorkflowEntitlement;
import java.lang.Override;
import java.lang.String;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

@Generated(
    value = "com.mercuria.dali.annotationprocessors.RestClientGenerator",
    date = "2020-04-27T18:09:05.189+01:00[Europe/London]"
)
public class RetrofitEntitlementsServiceClient implements EntitlementsServiceClient {
  private static final Logger LOGGER = LoggerFactory.getLogger("com.mercuria.dali.rest.RetrofitEntitlementsServiceClient");

  private final RetrofitEntitlementsServiceClientApi clientApi;

  private final ObjectMapper objectMapper;

  private final int maxRetries;

  private RetrofitEntitlementsServiceClient(RetrofitEntitlementsServiceClientApi clientApi,
      ObjectMapper objectMapper, int maxRetries) {
    this.clientApi = clientApi;
    this.objectMapper = objectMapper;
    this.maxRetries = maxRetries;
  }

  public static final RetrofitWebServiceClientBuilder<RetrofitEntitlementsServiceClientApi, RetrofitEntitlementsServiceClient> createBuilder(
      String baseUrl) {
    return new RetrofitWebServiceClientBuilder<RetrofitEntitlementsServiceClientApi, RetrofitEntitlementsServiceClient>(baseUrl, LOGGER, RetrofitEntitlementsServiceClientApi.class, RetrofitEntitlementsServiceClient::new);
  }

  public static final RetrofitEntitlementsServiceClient create(String baseUrl) {
    return createBuilder(baseUrl).build();
  }

  @Override
  public Set<WorkflowEntitlement> getRoleResourceActionMapping(String domain) {
    Supplier<Call<Set<WorkflowEntitlement>>> responseSupplier = () -> this.clientApi.getRoleResourceActionMapping(domain);
    return RetrofitHelper.getValue(LOGGER, responseSupplier, false, this.maxRetries);
  }
```
