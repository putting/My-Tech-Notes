# Point & Distance

## Description
Imagine you have been given a list of 20 customer's including their address details.
Write an algorithm to work out the closest (by address) to the main delivery depot.

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
