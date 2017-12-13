# Point & Distance

## Description
Imagine you have been given a list of 20 customer's names including their address details.
A manager is in town and only has time to visit the nearest 20 customers.
Write an algorithm to work out the closest (by address) to the manager.

## Assumptions
- legacy table, can't change by adding columns etc..
- small dataset to begin with. say 100 rows
- Geolocations service. Initially can be part of solution, BUT then seen as separate service and maybe hosted externally.
    - This is part of the 'how long to run' question
    - Batching of request rather than single requests
- Performance: Sorting (depending onn alg O(n2))  [http://bigocheatsheet.com/](http://bigocheatsheet.com/)

## Steps
- Take the data into a structure
- Calculate the distance from begining point to each (and hold against name and maybe address)
- Can then just sort and take first 10

## Scala Fold design
- Take first n records, then fold...checking if any of the next n are samller then swap etc..** O(log n)**

We have an List of the following. So these need to be translated into a Customer/Point objects. These objects may well store distanceFromDepot to enable sorting.

```java
class Customers {
private String name;
private Address address; //This will be in the 20 A stret, london, EC1 format
}
```

## Testing the following
- Ability to transalate an adddress into a computer useful structure. ie. Point
- Point should have a method determin the distance between Points.
- Recogniton of sorting and getting Top 20 of something using java streams.
- **Performance:** Say Service call to get Distance. ie Point/Distance is a remote service
    - What ways are there to call a remote service? i.e REST etc..????
    - Batching calls for al pointss in one call.
    - How long will the code take to run?
        - ie What is the bottleneck. network, disk, individual calls
        - Monitoring/stats to identify slow points.
        - Big O for some parts???


``` java
public static class Point
    {
        private int x;
        private int y;

        public Point(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        /**
         * For our purpose, square distance is good enough
         * It allows to avoid the costly square root
         *
         * @param p Point to which we want the distance
         * @return Distance between current and p
         */
        public int getDistanceTo(Point p)
        {
            int dx = this.x - p.x;
            int dy = this.y - p.y;
            return dx * dx + dy * dy;
        }

        @Override
        public String toString()
        {
            return "(" + x + ", " + y + ')';
        }
    }
```
