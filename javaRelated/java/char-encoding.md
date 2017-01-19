# Character Encoding

## Encoding Schemes : How to map number to bytes
UTF-8 variable encoding uses: min 1 byte. Only code points 128 (**up to 127 1 byte**) and above are stored using 2,3 or in fact, up to 4 bytes.
UTF-16 : variable 2, 4 bytes depending
UTF-32 **FIXED with 4** bytes per char

## Charset
Many different characters in world MAPPED to NUMERIC value (code points).

## egs
Capital A : Unicode code point is U+0041, UTF-8 = 41, UTF-16 = 0041

UTF-8 has an advantage where ASCII are most used characters, in that case most characters only need one byte.

## Part 2
[link](https://www.ibm.com/developerworks/library/j-codetoheap/)
[also some interesting stuff here including optimising string of 2 repeating values using hashMap](http://www.javaworld.com/article/2077496/testing-debugging/java-tip-130--do-you-know-your-data-size-.html)

## i) 32-bit processor
int = 4 bytes
Integer = class pointer (4 bytes) + flags (state of obj 4 bytes) + locks (4 bytes) = int (data 4 bytes) = 16 bytes !
Array = as Integer + size (4 bytes) = 20 bytes
String = as Integer + hash (4b) + count (4b) + offset (4b) + char array of data
                Char array of data = as Array + 2 bytes per each char (UTF-8). 

Eg String “a” = **46 bytes. Only 2 bytes are the actual single char!!!**

## ii) 64 bit processor
int = 28 bytes (as opposed to 4 bytes in 32bit pc)
int Array = 36 bytes  (as opposed to 20 bytes)


