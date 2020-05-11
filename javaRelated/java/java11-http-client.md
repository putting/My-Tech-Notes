# Java 11 includes an http client - useful for REST services

https://dzone.com/articles/java-11-http-client-api-to-consume-restful-web-ser-1

more egs: https://openjdk.java.net/groups/net/httpclient/recipes.html

## The Eg uses Spring Boot
Things of note:
- DAO setup and mapping is easy if you use JPA
- Jackson utils class (see below)
- Db class (Dto) ProductEntity with javax.persistance annotations
- ProductDAOWrapper maps between the ProductEntity (dto) and ProductBean. NB ** Spring  BeanUtils.copyProperties(x, product); **
It woud be nice if there was one immutable dto/db object.
- client.sendAsync. Then response.get is the blocking wait fore response.
- TypeReference
- HttpResponse **.join** is like .get for asyc requests BUT throws UNchecked Ex. BOTH return the result of last competion stage.
```java
CompletableFuture<HttpResponse<String>> response = client.sendAsync(request,HttpResponse.BodyHandlers.ofString());
    if(response.get().statusCode() == 500)
        System.out.println("Product Not Avaialble to update");
    else {
        bean = JSONUtils.covertFromJsonToObject(response.get().body(), ProductBean.class);
        System.out.println(bean);
    }
    response.join();
```


## Java 11 modules

Need to add Jackson-annotations-2.7.3, Jackson-core-2.7.3, Jackson-databind-2.7.3 jar files in the module path.

## Jackson Converter  Utils for Json
```java
public class JSONUtils {
    //convert JSON into List of Objects
    static public <T> List<T> convertFromJsonToList(String json, TypeReference<List<T>> var) throws JsonParseException, JsonMappingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, var);
    }
    //Generic Type Safe Method – convert JSON into Object
    static public <T> T covertFromJsonToObject(String json, Class<T> var) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, var);//Convert Json into object of Specific Type
    }
    //convert Object into JSON
    public static String covertFromObjectToJson(Object obj) throws JsonProcessingException{
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}
```
