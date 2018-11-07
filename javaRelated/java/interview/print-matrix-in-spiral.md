# Print a Square Matrix in Spiral Form (40min)
Write a class that, given a positive integer N, creates and returns a square matrix NxN made out of integers that displays the 
numbers from 1 to n^2 in a spiral

1,2,3,4
12,13,14,5
11,16,15,6
10,9,8,7

## Attributes of solution
- Store in 2 dim array
- The solutions below break the problem into writing 4 sides. Which then get looped.
- There is a direction
- There should be something to prevent overwrites of boundary and populated squares.

I think you could also implement by:
- establishing that the direction is always E,S,W,N (maybe assign an integer).
  - Depending on the direction you inX(E), incY(s), decX(W), decY(N)


## Sample answers
[Sample Answer](http://theoryofprogramming.com/2017/12/31/print-matrix-in-spiral-order/)
[Python-more complex]https://coderbyte.com/algorithm/print-matrix-spiral-order)

```java
public static void printInSpiralOrder(final int[][] arr) {
    if (arr.length == 0 || arr[0].length == 0) {
        return;
    }
 
    int top = 0, bottom = arr.length - 1, left = 0, right = arr[0].length - 1;
    int dir = 1;
 
    while (top <= bottom && left <= right) {
        if (dir == 1) {    // left-right
            for (int i = left; i <= right; ++i) {
                System.out.print(arr[top][i] + " ");
            }
 
            ++top;
            dir = 2;
        } else if (dir == 2) {     // top-bottom
            for (int i = top; i <= bottom; ++i) {
                System.out.print(arr[i][right] + " ");
            }
 
            --right;
            dir = 3;
        } else if (dir == 3) {     // right-left
            for (int i = right; i >= left; --i) {
                System.out.print(arr[bottom][i] + " ");
            }
 
            --bottom;
            dir = 4;
        } else if (dir == 4) {     // bottom-up
            for (int i = bottom; i >= top; --i) {
                System.out.print(arr[i][left] + " ");
            }
 
            ++left;
            dir = 1;
        }
    }
}
```

