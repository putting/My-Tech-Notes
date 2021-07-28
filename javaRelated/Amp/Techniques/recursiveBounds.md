# Recursive Generic Type Bounds

Interesting recursive approach. Used typically in abstract classes or parent classes.
It defines a bound for child classes so that:
- the same types are used for a comparison (method in parent). `eg cap.CompareTo(bus) is wrong
- The correct subType is used rather than Object (ie unbounded types). T is erased to Object.

simplest eg from java api, where a class is comparable to itself. which is what you want usually for comparators. This eg does not use inheritance though.
`public class SampleClass<E extends Comparable<SampleClass>>`

## Examples

Examples of a Car with compareTo of different Generic Types in parent not allowed
https://dzone.com/articles/introduction-generics-java-%E2%80%93-0

Follow up of mistake in child defn of generic param
https://dzone.com/articles/introduction-generics-java-%E2%80%93-1

## Amp egs
NB. If we remove the bound from BusinessDto then there is a compiule error in `CommApiDto.<T>builder()`
This is because the ComApiDto has the Business bound and is required to have T BusinessDto payload.
Note the cast (T) to self which MUST be added to a CommApi of the SAME type.Also the compiler compiler does not know whether or not this conversion is possible, even though it is because T by definition is bounded by BusinessDto<T>
```java
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
and child passes its own Type as Generic Param
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
@ApiModel(description = "Data transfer object for cost. To be encapsulated into a wrapper DTO",
        parent = BusinessDto.class)
public class CostDto implements BusinessDto<CostDto> {
...etc
```
```
and this T extends the BusinessDto not itself, which is fine as its the business payload
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

### From Kafka
Here C and R are the child types of this class
```java
/**
 * Basis for Kafka configurers that wire up a k-stream that loads a collection of outputs for each input.
 * <p></p>
 * Each element in the input stream configured by this class represents a collection of entities to load from an external
 * medium. For performance reasons, loading takes place in parallel and it is optionally cached.
 * <p></p>
 * This configurer sets up a stream that does the following:
 * <ol>
 *     <li>Receive each source element and, if not cached, invoke loading the results from the external medium in parallel. The
 *     level of concurrency does not exceed the number of processors.</li>
 *     <li>Flatten the stream of lists of results into a stream of result entities</li>
 *     <li>Publish the resulted stream to the destination topic</li>
 * </ol>
 *
 * @param <C>  type of current instance.
 * @param <KT> type of source key.
 * @param <T>  type of source entity.
 * @param <KR> type of destination key.
 * @param <R>  type of destination entity.
 */
@Slf4j
/* @Component */
public abstract class AbstractSimpleExternalFetchKafkaStreamsConfigurer<
        C extends AbstractSimpleExternalFetchKafkaStreamsConfigurer<C, KT, T, KR, R>,
        KT extends Comparable<KT>,
        T,
        KR extends Comparable<KR>,
        R> extends AbstractKafkaStreamSingleSourceAndResultConfigurer<C, KT, T, KR, R> {
```
