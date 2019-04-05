# Retrofit

A type-safe HTTP client for Android and Java
Retrofit is a REST Client for Java and Android. It makes it relatively easy to retrieve and upload JSON (or other structured data) via a REST based webservice. In Retrofit you configure which converter is used for the data serialization. Typically for JSON you use GSon, but you can add custom converters to process XML or other protocols. Retrofit uses the OkHttp library for HTTP requests.

[retrofit](http://square.github.io/retrofit/)
[Tutorial](https://www.vogella.com/tutorials/Retrofit/article.html)

## Retrofit turns your HTTP API into a Java interface.

```1java
public interface GitHubService {
  @GET("users/{user}/repos")
  Call<List<Repo>> listRepos(@Path("user") String user);
}
```
