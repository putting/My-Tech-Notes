# 2nd largest number in array

## The Question
Find Second Largest Number in an Array:

int a[]={1,2,5,6,3,2};

public static int getSecondLargest(int[] a){

//TODO: Implement

}

## Without collections
```
public static int getSecondLargest(int[] a, int total){  
int temp;  
for (int i = 0; i < total; i++)   
        {  
            for (int j = i + 1; j < total; j++)   
            {  
                if (a[i] > a[j])   
                {  
                    temp = a[i];  
                    a[i] = a[j];  
                    a[j] = temp;  
                }  
            }  
        }  
       return a[total-2];  
}  
```

## Using Arrays or collections

```
public static int getSecondLargest(int[] a, int total){  
Arrays.sort(a);  
return a[total-2];  
} 
```
or
```
public static int getSecondLargest(Integer[] a, int total){  
List<Integer> list=Arrays.asList(a);  
Collections.sort(list);  
int element=list.get(total-2);  
return element;  
} 
```
