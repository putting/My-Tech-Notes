# Payloads


## Main Message or DTO
NB. Its Generic supporting multiple Business Types, which extend BusinessDto for the payload
*Interesting is the ControlDto, manages versiosning etc.. see below*
Also Use of swagger to document the schema

```java
@Data
@Builder
@FieldNameConstants
@ApiModel(description = "Generic wrapper for all the business DTOs")
public class CommApiDto<T extends BusinessDto<T>> implements Serializable {
    private static final long serialVersionUID = 8266144408906419033L;

    @ApiModelProperty("Metadata associated with each event")
    @NotNull
    private ControlDto control;
    @ApiModelProperty("Embedded DTO carrying business-relevant information")
    @NotNull
    private T          businessPayload;
}
```

## ControlDTO
So Tx, not sure if from db or created for a unit of work.
Interesting use of apiName and version. Maybe supports evolving schemas?

```java
@Data
@Builder
@FieldNameConstants
@ApiModel(description = "Contains information about the event being consumed",
          discriminator = ControlDto.Fields.apiName)
public class ControlDto implements Serializable {
    private static final long serialVersionUID = 583698469817485281L;

    @ApiModelProperty("Unique transaction event identifier")
    @Min(1L)
    private long   transactionEventId;
    @ApiModelProperty("Transaction event vent timestamp in ISO 8601 extended format. It includes time zone information")
    @NotNull
    @NotEmpty
    @NotBlank
    private String transactionEventTimeStamp;
    @ApiModelProperty("Name of API producing the event")
    @NotNull
    @NotEmpty
    @NotBlank
    private String apiName;
    @ApiModelProperty("Version of the API producing the event")
    @NotNull
    @NotEmpty
    @NotBlank
    private String apiVersion;
}
```

## BusinessDto
NB. Its an interface where T extends BusinessDto<T>
```java
@ApiModel(description = "Marker for all the DTOs carrying business meaning. See '" + ControlDto.Fields.apiName + "' for type discriminator",
          discriminator = ControlDto.Fields.apiName,
          subTypes = {
                  TradeItemDto.class,
                  ActualDto.class,
                  InventoryDto.class,
                  CostDto.class,
                  FinalLiquidationDto.class,
                  FinalStatementDto.class,
                  TagDto.class
          })
public interface BusinessDto<T extends BusinessDto<T>> extends Serializable {

    /**
     * Wraps the current instance into a {@link CommApiDto} by attaching a {@link ControlDto} to it.
     *
     * @param control control DTO to attach.
     *
     * @return wrapping instance.
     */
    default CommApiDto<T> withControl(@NonNull ControlDto control) {
        final var self = (T) BusinessDto.this;
        final var result = CommApiDto.<T>builder()
                                     .control(control)
                                     .businessPayload(self)
                                     .build();
        return result;
    }
}
```
  
  ## ClientApiInfo
  
  ```java
  /**
 * Bean containing information about the APIs to be consumed by the clients of our services.
 */
/* @Component */
@ToString
public class ClientApiInfo {

    public static final String NAME_ROOT_PROPERTY = "client.api.info.name.root";
    public static final String VERSION_PROPERTY   = "client.api.info.version";

    public static final String API_NAME_SUFFIX = "-api";

    @Value("${" + NAME_ROOT_PROPERTY + "}")
    private String nameRoot;
    @Getter
    @Value("${" + VERSION_PROPERTY + "}")
    private String version;

    /**
     * Gets the name of the API, including the suffix.
     *
     * @return api name, with suffix.
     */
    public String getName() {
        return nameRoot.concat(API_NAME_SUFFIX);
    }
}
  ```
