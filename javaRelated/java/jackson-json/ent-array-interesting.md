# Based on an Entitlements change using Array rather than single object

This allows the system to submit a more compact json, which gets converted into existing dbo's to update the db.
The coversion is managed by:
- Creating a new object CompressedEntitlement.class
- Create a custome Serialiser which extends Jacksons StdDeserializer
- Annotate the EntitlementRequest.class to use the custom deserializer: 
  @JsonDeserialize(using = EntitlementDeserializer.class)
  List<Entitlement> entitlements();
- Map the CompressedEntitlent back into List<Entitlement> for persistance


## CompressedEntitlement.class
Note the jackson JsonFormat which will convert from the existing Entitlement.class single entry to an array

```java
@Value.Immutable
@JsonSerialize(as = ImmutableCompressedEntitlement.class)
@JsonDeserialize(as = ImmutableCompressedEntitlement.class)
public interface CompressedEntitlement {

    String roleName();

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    List<String> resourceGroupName();

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    List<String> actionName();

}

```

## EntitlementDeserializer
This reads all the entitlements json

```java
public class EntitlementDeserializer extends StdDeserializer<List<Entitlement>> {

    public EntitlementDeserializer() {
        this(null);
    }

    protected EntitlementDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<Entitlement> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ImmutableList.Builder<Entitlement> listBuilder = new ImmutableList.Builder<>();

        JsonNode entitlementsNode = jp.getCodec().readTree(jp);
        Iterator<JsonNode> it = entitlementsNode.elements();
        while (it.hasNext()) {
            CompressedEntitlement compressedEntitlement = jp.getCodec().treeToValue(it.next(), CompressedEntitlement.class);
            listBuilder.addAll(EntitlementMapper.decompressEntitlement(compressedEntitlement));
        }

        return listBuilder.build();
    }
}

```

## EntitlementMapper
Note use of ImmutableList from google.

```java
public class EntitlementMapper {

    /**
     * Turns the Compressed Entitlement into individual entitlements
     */
    public static List<Entitlement> decompressEntitlement(CompressedEntitlement compressedEntitlement) {
        ImmutableList.Builder listBuilder = new ImmutableList.Builder<Entitlement>();
        for (String resourceGroup : compressedEntitlement.resourceGroupName()) {
            for (String action : compressedEntitlement.actionName()) {
                listBuilder.add(ImmutableEntitlement.builder()
                        .roleName(compressedEntitlement.roleName())
                        .resourceGroupName(resourceGroup)
                        .actionName(action)
                        .build());
            }
        }
        return listBuilder.build();
    }


}

## Sample of new json
Mixes single entries and arrays

```csv
....prev parts not shown
"entitlements": [
    {
      "roleName": "TEST_ROLE",
      "resourceGroupName": "TEST_RESOURCE_GROUP_1",
      "actionName": "TEST_READ"
    },
    {
      "roleName": "TEST_ROLE2",
      "resourceGroupName": ["TEST_RESOURCE_GROUP_1", "TEST_RESOURCE_GROUP_2"],
      "actionName": ["TEST_READ", "TEST_WRITE"]
    },
    {
      "roleName": "TEST_ROLE3",
      "resourceGroupName": "TEST_RESOURCE_GROUP_1",
      "actionName": ["TEST_READ", "TEST_WRITE"]
    }
  ]
```

```
