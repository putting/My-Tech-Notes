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
``
