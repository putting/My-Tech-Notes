# General Notes on JS

## Book: Rediscovering...
### p81: Immutability of objects: Object.freeze

There is a caveat in that it is a shallow freeze. Any props refering to another obj are not frozen.
```js 
const sam = Object.freeze({ first: 'Sam' , age: 2 });
```

### Prefer for .. in loops over traditional loops. You can use a const too.
```js
for (const in arr) {consolde.log(arr[i])}
```

### Rest parm and Spread operator

const myFn = function(...rest) defines a fn which takes an array of params.
So all the array ops are available.
There can only be 1 rest param and needs to be last in list

*Spread Op* uses the same ... but is when calling fns. 

It takes arrays and flattens then into multiple items.
But can also
- Copy/Concatenate arrays: log([...names1, 'Brooke' ]);
- Copy objects and replace values: 
```js
const sam = { name: 'Sam' , age: 2 };
console.log(sam);
console.log({...sam, age: 3}); //copies and updates the age property
```

### Sort and Slice and Fn with default value
Sort - changes the array being sorted.
Slice - Makes a copy of an array.
localeCompare - compares in the locale of the browser.
default param - makes the fn more generic. BUT then you could pass in the fn to sort as a param...

```js
const sortByTitle = function (books, ascending = true ) {
  const multiplier = ascending ? 1 : -1;
  const byTitle = function (book1, book2) {
    return book1.title.localeCompare(book2.title) * multiplier;
  };
  return books.slice().sort(byTitle);
};
```

## Symbol
- Can be used for enums as unique
- Implementing in-built methods. eg iterator. So a custom class can support iteration. see p154 of Rediscovering..
  [Symbol.iterator]() {.....
- Providing an interface-like object

## Generastors
A custom class can be converted to provide iteration by combining Symbol.iterator method and a Generator (see *).
Yield pauses exec returning value to caller, waiting for next() to be called.
```js
*[Symbol.iterator]() {  //could replace with *suits() {.. but then need to call for ( const suit of deck.suits()) {..
for ( const shape of this.suitShapes) {
yield shape;
}
}
```

