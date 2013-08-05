DiskBasedArray
==============

This is a Java MMap based two-dimensional primitives array implementation. 
This currently supports only <b>int[][]</b> array. 

I needed to work with int arrays, but was restricted by the size of the Java heap, when it came to creating an array containing a few million elements. 
Additionally, I needed a 2D array - int[x][y] - where x could be in the millions, and y varies and is not a constant for all the elements of the array. 
