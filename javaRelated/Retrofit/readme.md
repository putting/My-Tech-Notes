# Retrofit

A type-safe HTTP client for Android and Java

[retrofit](http://square.github.io/retrofit/)


## Retrofit turns your HTTP API into a Java interface.

```1java
public interface GitHubService {
  @GET("users/{user}/repos")
  Call<List<Repo>> listRepos(@Path("user") String user);
}
```
