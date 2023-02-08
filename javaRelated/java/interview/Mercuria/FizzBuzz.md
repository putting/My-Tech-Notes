# FizzBuzz

## The Question

The rules of the FizzBuzz game are very simple. Iterate through integers up to a number given and map to FizzBuzz etc....
     * Say Fizz if the number is divisible by 3.
     * Say Buzz if the number is divisible by 5.
     * Say FizzBuzz if the number is divisible by both 3 and 5.
     * Return the number itself, if the number is not divisible by 3 and 5

```
class Mercuria
{

    /**
     Given an integer n, return a string list where:

     list[i] == "Mercuria" if i is divisible by 3 and 5.
     list[i] == "Merc" if i is divisible by 3.
     list[i] == "uria" if i is divisible by 5.
     list[i] == i (as a string) if none of the above conditions are true.
     Example     
        Input: upToAndIncluding = 5
        Output: ["1","2","Merc","4","uria"]
     **/
    public static List<String> calculate(Integer upToAndIncluding) {
        // implement
    }

    /**
     Given a list of Strings count the occurrences per string
     - if string == "secret" it should be excluded from the counting
     **/
    public static Map<String, Long> countMercurias(List<String> listOfStrings) {
        // implement
    }

    private static String smt(int i) {
    }


    /**
     Test code
     **/

    public static void main(String args[])
    {

        var result = calculate(20);
        checkResults(result, Arrays.asList("Mercuria","1","2","uria","4","Merc","uria","7","8","uria","Merc","11","uria","13","14","Mercuria","16","17","uria","19","Merc"));

        var stringLongMap = countMercurias(Arrays.asList("Mercuria", "Mercuria", "secret", null, "secret", "Mercuria"));

        if (!stringLongMap.get("Mercuria").equals(3L)) {
            throw new RuntimeException("Mercuria count should be 3");
        }

        if (stringLongMap.containsKey("secret")) {
            throw new RuntimeException("secret should be included in the calculation");
        }

        var calculateResultNull = calculate(null);
        checkResults(calculateResultNull, List.of());
    }

    private static void checkResults(List<String> list, List<String> expected) {
    }
}
```

## The Solution
```
package com.mercuria.marketdata.curveservice.server.contractexpiries;

import java.util.*;
import java.lang.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class Mercuria
{

    /**
     Given an integer n, return a string list where:

     list[i] == "Mercuria" if i is divisible by 3 and 5.
     list[i] == "Merc" if i is divisible by 3.
     list[i] == "uria" if i is divisible by 5.
     list[i] == i (as a string) if none of the above conditions are true.
     **/
    public static List<String> calculate(Integer upToAndIncluding) {

        if(upToAndIncluding == null) {
            return List.of();
        }

        var list = new ArrayList<String>();

        for (int i = 0; i <= upToAndIncluding; i++) {
            list.add(smt(i));
        }


        // implement
        return list;
    }

    /**
     Given a list of Strings count the occurrences per string
     - if string == "secret" it should be excluded from the counting
     **/
    public static Map<String, Long> countMercurias(List<String> listOfStrings) {
        // implement
        return listOfStrings.stream()
                .filter(Objects::nonNull)
                .filter(s -> !"secret".equals(s))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static String smt(int i) {
        if(i % 5 ==0 && i %3==0) {
            return "Mercuria";
        }else         if(i % 5 ==0 ) {
            return "Merc";
        }else         if( i %3==0) {
            return "uria";
        }
        return String.valueOf(i);
    }
    
    //Using Streams
    public static List<String> calcStreams(int num) {
        List<String> result = IntStream.rangeClosed(1, num)
                .mapToObj(i -> i % 3 == 0 ? (i % 5 == 0 ? "FizzBuzz " : "Fizz ") : (i % 5 == 0 ? "Buzz " : i + " "))
                .collect(Collectors.toList());
        return result;
    }

    /**
     Test code
     **/

    public static void main(String args[])
    {

        var result = calculate(20);
        checkResults(result, Arrays.asList("Mercuria","1","2","uria","4","Merc","uria","7","8","uria","Merc","11","uria","13","14","Mercuria","16","17","uria","19","Merc"));

        var stringLongMap = countMercurias(Arrays.asList("Mercuria", "Mercuria", "secret", null, "secret", "Mercuria"));

        if (!stringLongMap.get("Mercuria").equals(3L)) {
            throw new RuntimeException("Mercuria count should be 3");
        }

        if (stringLongMap.containsKey("secret")) {
            throw new RuntimeException("secret should be included in the calculation");
        }

        var calculateResultNull = calculate(null);
        checkResults(calculateResultNull, List.of());
    }

    private static void checkResults(List<String> list, List<String> expected) {
        for (int i = 0; i < expected.size(); i++) {
            if(!Objects.equals(list.get(i), expected.get(i))) {
                throw new RuntimeException("expected["+i+"]: " + expected.get(i) + " does not match actual " +list.get(i) );
            }
        }
    }
}

```
