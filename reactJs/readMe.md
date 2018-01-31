# ReactJs

## Learning React Book
- p196: React is a lib for **viewing** data.
  Frameworks such as Angular and jQuery come with their own tools for accessing data, rendering the
UI, modeling state, handling routing, and more. React, on the other hand, is simply a library for
creating views, so we may need to work with other JavaScript libraries.
- p198: Mapping json to an array
 Note how simple doing map from json extracts the name from each object in the json and puts in an array
```javascript
fetch('https://restcountries.eu/rest/v1/all')
.then(response => response.json())
.then(json => json.map(country => country.name))
.then(countryNames =>
this.setState({countryNames, loading: false})
)
```


### React and JQuery
Using jQuery with React is generally frowned upon by the community. It is possible to integrate jQuery and React, and the
integration could be a good choice for learning React or migrating legacy code to React. However, applications perform
much better if we incorporate smaller libraries with React, as opposed to large frameworks. Additionally, using jQuery to
manipulate the DOM directly bypasses the virtual DOM, which can lead to strange errors.
