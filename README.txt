Task 1

This task has three .java files, which is btindex.java, InternalNode.java and LeafNode.java. The three files should be together. The heap.xxx file created by task 1 in Assignment is also needed.

The command for running it is showed below:
java btindex -p <pagesize> <heapfile>

For example,
java btindex -p 4096 heap.4096

===================================================

Task 2

This task has only one file which is btsearch.java. It needs the heap file and index file to run. 

The command for running it is showed below:
java btsearch <heapfile> <indexfile> <start date> <end date>

For example,
java btsearch heapfile.4096 index.4096 19700101 19701231
