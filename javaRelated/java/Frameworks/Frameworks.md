# Frameworks

###Spark Web Framework - interesting for lightwieght RESTful  web services
[link](https://tomassetti.me/getting-started-with-spark-it-is-possible-to-create-lightweight-restful-application-also-in-java/)

Illustrates declarative rather than programmatic ways of validating incoming json:

Create a class representing the incoming post (like a dto) payload
And then use Jackson object mapper
ObjectMapper mapper = new ObjectMapper();
   NewPostPayload newPost = mapper.readValue(request.body(), NewPostPayload.class);

This part means **Jackson will check the json structure**, but then we would need to validate payload.

Add implaements Validable to payload and include methods like isValid.

Next improvement is to use LOMBOK annotations.
