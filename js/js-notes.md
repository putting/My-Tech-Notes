# General Notes on JS

## Book: Rediscovering...
p81: Immutability of objects: Object.freeze
There is a caveat in that it is a shallow freeze. Any props refering to another obj are not frozen.
```js 
const sam = Object.freeze({ first: 'Sam' , age: 2 });
```
