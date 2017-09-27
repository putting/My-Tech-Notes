# Reading from a config or properties file

## Properties file reading. 

```java
        String env = Optional.ofNullable(System.getenv("SYS_ENV")).orElse("local");
        URL resource = Resources.getResource("config/" + env + "/" + env + ".properties");
        HashMap<String, String> properties = Maps.newHashMap();
        try (InputStream inputStream = resource.openStream()) {
            Properties utilProperties = new Properties();
            utilProperties.load(inputStream);
            utilProperties.forEach((o, o2) -> properties.put(o.toString(), o2.toString()));
        }
```
## Yaml file reading

```java
   StrSubstitutor strSubstitutor = new StrSubstitutor(properties);
        strSubstitutor.setEnableSubstitutionInVariables(true);
        String yamlString = Resources.toString(Resources.getResource("giantSpark.yml"), Charset.defaultCharset());
        String subYamlString = strSubstitutor.replace(yamlString);
        ObjectMapper configOM = new ObjectMapper(new YAMLFactory());
        return configOM.readValue(subYamlString, Configuration.class);
```
