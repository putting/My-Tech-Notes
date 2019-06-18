# Point & Distance

## Description
Imagine you have been given a list of 100 customer's names including their address details.
A manager is in town and only has time to visit the nearest 20 customers.
Write an algorithm to work out the closest (by address) to the manager.
This is similar to the Travelling Salesman Problem (TSP), but MUCH simpler as we are only asking to find nearest n, NOT find the shortes/quickest route through each location.

### TSP
[TSP](https://simple.wikipedia.org/wiki/Travelling_salesman_problem)
Based on graph theory of nodes and edges (lines)
also [java solution](https://www.baeldung.com/java-simulated-annealing-for-traveling-salesman) 

###
[ArcGIS good expl](https://developers.arcgis.com/java/10-2/guide/search-for-places-and-addresses.htm)
The process of transforming an address or place name to a geographical location on a map is known as **geocoding**. The opposite task, searching for the nearest address to a particular point on a map using geographic coordinates, is referred to as **reverse geocoding**. 
The primary purpose of geocoding services is to convert an address to an x,y coordinate.
Describes Geocoding in detail.

## Assumptions
- legacy table, can't change by adding columns etc..
- small dataset to begin with. **say 100 rows**
- Geolocations service. Initially can be part of solution, BUT then seen as separate service and maybe hosted externally.
    - This is part of the 'how long to run' question
    - Batching of request rather than single requests
- Performance: Sorting (depending onn alg O(n2))  [http://bigocheatsheet.com/](http://bigocheatsheet.com/)

## Steps
- Read the data from a db table. jdbc, dao, (all records)
- Take the data into a structure - A DTO or CustomerDbo (with Mapper).
- Do they create an immutable EnhancedCustomer with distance?
- Calculate the distance from begining point to each (and hold against name and maybe address. 
    Ask if they know how that distance might be calculated in lib. ie Pythagoras and
    what assumptions that involves in practice. ie. flat earth not curved. 
    use lib in final sol)
- Can then just sort (with Comparator or Comparable) and take first 10.
    A TreeMap is sometimes suggested (Red Black Tree). Requires Comparable or Comparator for keys.
    - guaranteed log(n) time cost for the containsKey,get,put and remove operations
    - If they use a Map and the key is a Double. What happens with 2 identical distances?
- What is returned? Should just be the Customer (no need to expose internal distance). ie stream.map.

## Scala Fold design
- Take first n records, then fold...checking if any of the next n are samller then swap etc..** O(log n)**

We have an List of the following. So these need to be translated into a Customer/Point objects. These objects may well store distanceFromDepot to enable sorting.

### Data: client address Table
```csv
client Id, Client Name, Client Address
1, client 1, 1 main road, london, ec1
2, client 2, 34 london road, london wc1
```

```java
class Customers {
private String name;
private Address address; //This will be in the 20 A stret, london, EC1 format
}
```

## Testing the following
- Ability to transalate an adddress into a computer useful structure. ie. Pointof Coordinate.
- Point (geocoding) should have a method determine the distance between Points.
- Recogniton of sorting and getting Top 20 of something using java streams.
- **Performance:** Say Service call to get Distance. ie Point/Distance is a **remote service**
    - How long ms/ 10 secs, 60 secs.... Where would the bottleneck be? (ie the http call)
    - What ways are there to call a remote service? i.e REST protocol etc..????
    - Batching calls for all points in one call.
    - How long will the code take to run?
        - ie What is the bottleneck. network, disk, individual calls
        - Monitoring/stats to identify slow points.
        - Big O for some parts???
    - Using a profiler to identify long-running methods.... 
    - Write tests and benchmarks and use logging.
        
### Follow up Question to create a System
Make this into a system. So this sales manager is happy, but the others want the same and they move around the country a lot.
- Requires an api, which allows the input of the Location to compare (this can used geoLocation from Phone)
- Requires a REST endpoint or similar
- Database optimisations? Saving geoLocation, batching calls to locationService, save all combinations of distance.
- Caching Service? What are the downsides of this: Keeping in sync with data, eviction etc..

### My first attempt a solution
```java
public class FindNearest {

    private List<Customer> customers = new ArrayList<>();

    public List<Customer> findNearest(int n) {

        //1. Create a starting point
        Point startingPoint = new Point(1,1);

        //2. Read the csv fil and transform into object. Maybe with
        customers.add(new Customer(1, "cust1", "1, london street", null));
        customers.add(new Customer(2, "cust2", "2, london street", null));
        customers.add(new Customer(3, "cust3", "3, london street", null));
        customers.add(new Customer(4, "cust4", "4, london street", null));
        customers.add(new Customer(5, "cust5", "5, london street", null));

        //3. Map the customer address to a geoLocation
        List<Customer> customers = mapCustomerLocations(this.customers);

        //4. Should then map this to a new object with distance from starting point
        //.....
        customers.forEach(c -> System.out.println(c.name + ":" + c.geoLocation.getDistanceTo(startingPoint)));

        //5. Then we can sort by distance lowest to highest.
        List<Customer> orderedCustomers = this.customers.stream()
                .sorted(Comparator.comparing((c1) -> c1.geoLocation.getDistanceTo(startingPoint)))
                .collect(Collectors.toList());

        return orderedCustomers;
    }

    private List<Customer> mapCustomerLocations(List<Customer> customers) {

        List<Customer> newCustomers = customers.stream()
                .map(c -> {
                    c.geoLocation = AddressMapper.map(c.address);
                    return c;
                })
                .collect(Collectors.toList());

        return newCustomers;
    }

    private static class AddressMapper {
        static Point map(String address) {
            Random random = new Random();
            int x = random.nextInt(10);
            int y = random.nextInt(10);
            return new Point(x,y);
        }
    }

}

public class Customer {

    public int id;
    public String name;
    public String address; //simple string for now...
    public Point geoLocation; //should really be immutable

    public Customer(int id, String name, String address, Point geoLocation) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.geoLocation = geoLocation;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", geoLocation=" + geoLocation +
                '}';
    }
}
```

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
         * For our purpose, square distance is good enough. Assumes the earth is flat rather than curved.
         * Should also apply square root to the result
         *
         * @param p Point to which we want the distance
         * @return Distance between current and p
         */
        public int getDistanceTo(Point p)
        {
            int dx = Math.abs(this.x - p.x)
            int dy = Math.abs(this.y - p.y)
            return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        }

        @Override
        public String toString()
        {
            return "(" + x + ", " + y + ')';
        }
    }
```

## Find closest k elements problem (includes java code)
[closest k problem](https://www.geeksforgeeks.org/find-k-closest-elements-given-value/)
A simple solution is to do linear search for k closest elements.
1) Start from the first element and search for the crossover point (The point before which elements are smaller than or equal to X and after which elements are greater). This step takes O(n) time.
2) Once we find the crossover point, we can compare elements on both sides of crossover point to print k closest elements. This step takes O(k) time.

The time complexity of the above solution is O(n).

An Optimized Solution is to find k elements in O(Logn + k) time. The idea is to use **Binary Search** to find the crossover point and uses a sorted array. Takes mid-point and checks against upper and lower half. Then chooses that half.. repeat. Once we find index of crossover point, we can print k closest elements in O(k) time.
