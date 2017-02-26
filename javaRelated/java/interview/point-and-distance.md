# Point & Distance


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
