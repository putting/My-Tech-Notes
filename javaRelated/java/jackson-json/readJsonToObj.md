# Simple eg of how to read json from File into an obj


```java
private static final ObjectMapper mapper = new ObjectMapper();
private final String collateralSrc = "src/test/resources/test-data/v_CUBE_collateral.json";

this.ictsCollateral = Arrays.asList(readJson(collateralSrc, StagingICTSCollateral[].class));

public static <T> T readJson(String fName, Class<T> tClass) {
        T data;
        try {
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new JavaTimeModule());
            mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
            data = mapper.readValue(new File(fName), tClass);
        } catch (IOException e) {
            throw new RuntimeException("Reading from " + fName + " failed", e);
        }
        return data;
    }
```

## Targe Object
```java
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mercuria.dali.jdbi.Jdbi;
import org.immutables.value.Value;

import java.time.LocalDate;
import java.util.Optional;

@Jdbi()
@Value.Immutable()
@JsonSerialize(as = ImmutableStagingICTSCollateral.class)
@JsonDeserialize(as = ImmutableStagingICTSCollateral.class)
public interface StagingICTSCollateral {

    int lcNum();

    String lcTypeCode();

    String lcExpImpInd();

    LocalDate lcStartDate();

    Optional<LocalDate> lcExpiryDate();

    String currency();

    Double lcAvailableAmt();

    String applicant();

    String beneficiary();

    String issuingBank();

    Optional<String> advisingBank();
}
```

## File source data
v_CUBE_collateral.json
```json
[
  {
    "lcNum": 9104,
    "lcTypeCode": "LFCEPWR ",
    "lcExpImpInd": "I",
    "lcStartDate": "2011-02-02",
    "lcExpiryDate": "2020-04-01",
    "currency": "EURO    ",
    "lcAvailableAmt": 1000000,
    "applicant": "MERCURIA GVA BC",
    "beneficiary": "APCS",
    "issuingBank": "MET GVA TRESO",
    "advisingBank": null
  },
  {
    "lcNum": 9887,
    "lcTypeCode": "CASHEGAS",
    "lcExpImpInd": "I",
    "lcStartDate": "2011-04-28",
    "lcExpiryDate": "2020-04-28",
    "currency": "EURO    ",
    "lcAvailableAmt": 136159,
    "applicant": "MERCURIA GVA BC",
    "beneficiary": "GAS CONNECT AT",
    "issuingBank": "MET GVA TRESO",
    "advisingBank": "FREE BNP 01"
  }
]
```
