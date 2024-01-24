import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class btindex {

	/*
	 * 1. personName -- 60 bytes 2. birthDate -- 16 bytes(8 bytes for the first date, 8 bytes for the 
	 * second date(if possible)) 3. birthPlace label -- 150 bytes 4. deathDate -- 16 bytes(same as birthDate)
	 * 5. field label -- 200 bytes 6. genre label -- 230 bytes 7. instrument label -- 256 bytes
	 * 8. nationality label -- 50 bytes 9. thumbnail -- 275 bytes 10. wikiPageID -- 4 bytes
	 * 11. description -- 335 bytes
	 * 
	 * record size = 1592
	 */
	
	/*
	 * internal node contains
	 * 1. flag used to represent internal node(1 byte)
	 * 2. n pointers to child nodes(4*n bytes)
	 * 3. n-1 key(8*(n-1) bytes)
	 * 
	 * so, pagesize = 1 + 4*n + 8*(n-1)
	 * 
	 * leaf node contains
	 * 1. flag used to represent leaf node(1 byte)
	 * 2. pointer to left leaf node(4 bytes)
	 * 3. pointer to right leaf node(4 bytes)
	 * 4. n data entry: (1) n key(8*n bytes) (2) n page id(4*n bytes) (3) n record id(4*n bytes)
	 * 
	 * so, pagesize = 1 + 4 + 4 + 8*n + 4*n + 4*n
	 * 
	 * Therefore, the maximum number of data entries a leaf node can contain is N = (pagesize - 9) / 16
	 * Since the number of records is 96300 and sometime the leaf node contain N/2 data entries, 
	 * which is the minimum number it can contain, so the maximum number of leaf nodes is 192600 / N
	 * 
	 * 
	 * One internal node can contain maximum (pagesize + 7) / 12 pointers to child nodes
	 * 
	 * Let x = the height of internal(excluding root node)
	 * ((pagesize + 7) / 12)^x = 192600 / N
	 * 
	 * Since the allowed minimum page size is 1592, N = 98.9375 ≈ 98
	 * so the maximum number of leaf nodes is 1966
	 * and one internal node can contain maximum 133 pointers to child nodes, but sometime it can take 
	 * a half number of maximum pointers which is 67 pointers
	 * 
	 * 67^x = 1966
	 * 
	 * the maximum x will be equal to 1.804, which is less than 2.
	 * So, the maximum number of internal nodes needed is 1966 / 67 + 1 = 30.34 ≈ 31(including root node)
	 * 
	 * 
	 */
	
	static int MAX_DATA_ENTRY_IN_LEAF_NODE;
	static int MAX_CHILD_NODES_IN_INTERNAL_NODE;
	final static int MAX_LEAF_NODE_NUM = 1966;
	final static int MAX_INTERNAL_NODE_NUM = 31;
	
	static LeafNode[] leafNodes = new LeafNode[MAX_LEAF_NODE_NUM]; // the set of leaf node
	static InternalNode[] internalNodes = new InternalNode[MAX_INTERNAL_NODE_NUM]; // the set of leaf node
	static int currentLocForL = 0; // the first empty position of the leafNodes array
	static int currentLocForI = 0; // the first empty position of the InternalNode array
	static int rootNodeLoc; // the location of the root node of the b+ tree
	
	static int heapPageId = 0; // page number of the heap file stored in data entries, start from 0
	static int heapRecordId = 0; // record number of the heap file stored in data entries, start from 0
	
	
	public static void main(String[] args) throws IOException {
		// check for correct number of arguments
        if (args.length != 3) {
            System.out.println("Error: Incorrect number of arguments were input");
            return;
        }
        
		int recordSize = 1592;
		int readBytes = 0;
		int indexedRecordNum = 0; // the number of records indexed(output to stdout)
		int pageSize = Integer.parseInt(args[1]);
		
		MAX_DATA_ENTRY_IN_LEAF_NODE = (int) Math.floor((pageSize - 9) / 16.0);
		MAX_CHILD_NODES_IN_INTERNAL_NODE = (int) Math.floor((pageSize + 7) / 12.0);
		
		FileInputStream in = null;
		FileOutputStream os = null;
		
		
		try {
			byte[] page = new byte[pageSize];
			in = new FileInputStream(args[2]);
			// 1. Firstly, fill the first leaf node
			LeafNode firstLeafN = new LeafNode(MAX_DATA_ENTRY_IN_LEAF_NODE);
			LeafNode secondLeafN = new LeafNode(MAX_DATA_ENTRY_IN_LEAF_NODE);
			int check = 0;
			long startTime = System.currentTimeMillis();
			while (in.read(page) != -1) {
				heapRecordId = 0;
				readBytes = 0;
				while (pageSize - readBytes > recordSize) {
					byte[] birthDate = new byte[8];
					// BirthDate start at 60th byte of every record
					System.arraycopy(page, 60 + readBytes, birthDate, 0, 8);
					long date = bytesToLong(birthDate);
					// If first birth date is -1, which represent null value, skip this record
					if (date == -1) {
						readBytes += recordSize;
						heapRecordId++;
						continue;
					}
					// If the first leaf node is full, split it into two leaf nodes and create an internal node
					if (firstLeafN.isFull()) {
						firstLeafN.insertDataEntry(date, heapPageId, heapRecordId);
						long[] keys = firstLeafN.getKeys();
						int[] heapPageIds = firstLeafN.getHeapPageIds();
						int[] heapRecordIds = firstLeafN.getHeapRecordIds();
						long[] temp1 = new long[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
						int[] temp2 = new int[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
						int[] temp3 = new int[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
						Arrays.fill(temp1, -1);
						Arrays.fill(temp2, -1);
						Arrays.fill(temp3, -1);
						int firstHalfLength = (int) Math.floor((MAX_DATA_ENTRY_IN_LEAF_NODE + 1) / 2.0);
						int secondHalfLength = keys.length - firstHalfLength;
						System.arraycopy(keys, 0, temp1, 0, firstHalfLength);
						System.arraycopy(heapPageIds, 0, temp2, 0, firstHalfLength);
						System.arraycopy(heapRecordIds, 0, temp3, 0, firstHalfLength);
						firstLeafN.setKeys(temp1);
						firstLeafN.setHeapPageIds(temp2);
						firstLeafN.setHeapRecordIds(temp3);
						firstLeafN.setCurrentLoc(firstHalfLength);
						firstLeafN.setFull(false);
						temp1 = new long[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
						temp2 = new int[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
						temp3 = new int[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
						Arrays.fill(temp1, -1);
						Arrays.fill(temp2, -1);
						Arrays.fill(temp3, -1);
						System.arraycopy(keys, firstHalfLength, temp1, 0, secondHalfLength);
						System.arraycopy(heapPageIds, firstHalfLength, temp2, 0, secondHalfLength);
						System.arraycopy(heapRecordIds, firstHalfLength, temp3, 0, secondHalfLength);
						secondLeafN.setKeys(temp1);
						secondLeafN.setHeapPageIds(temp2);
						secondLeafN.setHeapRecordIds(temp3);
						secondLeafN.setCurrentLoc(secondHalfLength);
 
						// Create the first internal node and make the this node point to these two leaf nodes
						// The reason for plus MAX_INTERNAL_NODE_NUM is that the leaf node is not at the 
						// first MAX_INTERNAL_NODE_NUM pages in the index file
						InternalNode firstInterN = new InternalNode(MAX_CHILD_NODES_IN_INTERNAL_NODE,
								secondLeafN.getKeys()[0], 0 + MAX_INTERNAL_NODE_NUM, 1 + MAX_INTERNAL_NODE_NUM);
						rootNodeLoc = 0; // the root node is now the internal node at the position 0 of the internal node array
						
						// connect first and second leaf node with each other
						firstLeafN.setRightPageId(1 + MAX_INTERNAL_NODE_NUM);
						secondLeafN.setLeftPageId(0 + MAX_INTERNAL_NODE_NUM);
						
						leafNodes[0] = firstLeafN;
						leafNodes[1] = secondLeafN;
						internalNodes[0] = firstInterN;

						currentLocForL+=2;
						currentLocForI++;
						
						indexedRecordNum++;
						readBytes += recordSize;
						heapRecordId++;
						check = 1;
						// insert the rest of records in this page into the b+ tree
						while (pageSize - readBytes > recordSize) {
							birthDate = new byte[8];
							// BirthDate start at 60th byte of every record
							System.arraycopy(page, 60 + readBytes, birthDate, 0, 8);
							date = bytesToLong(birthDate);
							// If first birth date is -1, which represent null value, skip this record
							if (date == -1) {
								readBytes += recordSize;
								heapRecordId++;
								continue;
							}
							if (date < internalNodes[0].getKeys()[0]) {
								firstLeafN.insertDataEntry(date, heapPageId, heapRecordId);
								firstLeafN.next();
							}
							else {
								secondLeafN.insertDataEntry(date, heapPageId, heapRecordId);
								secondLeafN.next();
							}
							indexedRecordNum++;
							readBytes += recordSize;
							heapRecordId++;
						}
						
					}
					// If the first leaf node is not full, insert the data entry into the first leaf node
					else {
						firstLeafN.insertDataEntry(date, heapPageId, heapRecordId);
						firstLeafN.next();
						indexedRecordNum++;
						readBytes += recordSize;
						heapRecordId++;
					}
				}
				heapPageId++;
				if (check == 1) {
					break;
				}
			}
			// ======================================================================
			// 2. After the first leaf node being split into two parts, we build the rest of the b+ tree based on this
			while (in.read(page) != -1) {
				heapRecordId = 0;
				readBytes = 0;
				while (pageSize - readBytes > recordSize) {
					byte[] birthDate = new byte[8];
					// BirthDate start at 60th byte of every record
					System.arraycopy(page, 60 + readBytes, birthDate, 0, 8);
					long date = bytesToLong(birthDate);
					// If first birth date is -1, which represent null value, skip this record
					if (date == -1) {
						readBytes += recordSize;
						heapRecordId++;
						continue;
					}
					insertToBPTree(date);
					indexedRecordNum++;
					readBytes += recordSize;
					heapRecordId++;
				}
				heapPageId++;
			}
			
			
			
			
			byte[] pageToWrite = new byte[pageSize];
			File indexFile = new File("index." + Integer.toString(pageSize));
			if (indexFile.exists()) {
				indexFile.delete();
			}
			indexFile.createNewFile();
			// The bytes will be written to the end of the index file every time 
			os = new FileOutputStream(indexFile, true);
			
			// 3. Write the internal nodes first
			// Firstly, write the page id of the root node
			os.write(intToBytes(rootNodeLoc));
			
			// The length of each node is greater than the length in theory, which is for convenience of 
			// programming, so we should delete the last elements in each each node
			for (int i = 0; i < currentLocForI; i++) {
				pageToWrite = new byte[pageSize];
				// flag for representing internal node
				pageToWrite[0] = 0;
				
				// keys
				byte[] keysArrBytes = longArrayToBytes(internalNodes[i].getKeys());
				System.arraycopy(keysArrBytes, 0, pageToWrite, 1, keysArrBytes.length - 8);
				
				// pointers to child nodes
				byte[] pointersArrBytes = intArrayToBytes(internalNodes[i].getChildPageId());
				System.arraycopy(pointersArrBytes, 0, pageToWrite, keysArrBytes.length - 7, pointersArrBytes.length - 4);
				
				os.write(pageToWrite);
			}
			
			// 4. Write the leaf nodes
			for (int i = 0; i < currentLocForL; i++) {
				pageToWrite = new byte[pageSize];
				// flag for representing leaf node
				pageToWrite[0] = 1;
				
				// pointer to left leaf node
				byte[] leftPointer = intToBytes(leafNodes[i].getLeftPageId());
				System.arraycopy(leftPointer, 0, pageToWrite, 1, 4);
				
				// pointer to right leaf node
				byte[] rightPointer = intToBytes(leafNodes[i].getRightPageId());
				System.arraycopy(rightPointer, 0, pageToWrite, 5, 4);
				
				// keys
				byte[] keysArrBytes = longArrayToBytes(leafNodes[i].getKeys());
				System.arraycopy(keysArrBytes, 0, pageToWrite, 9, keysArrBytes.length - 8);
				
				// page id
				byte[] pageIdArrBytes = intArrayToBytes(leafNodes[i].getHeapPageIds());
				System.arraycopy(pageIdArrBytes, 0, pageToWrite, 1 + keysArrBytes.length, pageIdArrBytes.length - 4);
				
				// record id
				byte[] recordIdArrBytes = intArrayToBytes(leafNodes[i].getHeapRecordIds());
				System.arraycopy(recordIdArrBytes, 0, pageToWrite, keysArrBytes.length + pageIdArrBytes.length - 3, recordIdArrBytes.length - 4);
				
				os.write(pageToWrite);
			}
			
			long endTime = System.currentTimeMillis();
			long usedTime = endTime - startTime;
			
			int height = (int) Math.ceil(Math.log(MAX_LEAF_NODE_NUM) / Math.log(67));
			System.out.println("The number of records indexed: " + indexedRecordNum);
			System.out.println("The number of index pages created: " + (currentLocForI + currentLocForL));
			System.out.println("The height of the B+-tree: " + height);
			System.out.println("The number of milliseconds to create the index file: " + usedTime);
			
		} catch (FileNotFoundException e) {
			System.err.println("Error: The heap file is not found!" + e.getMessage());
		} catch (IOException e) {
			System.err.println("Error: IOException" + e.getMessage());
		}
		finally {
			os.close();
		}
	}
	
	public static void insertToBPTree(long key){
		int currentPageId = rootNodeLoc; // first page should be the root node to start
		int pointerLoc = 0;
		ArrayList pageIdList = new ArrayList(); // the path of searching the key
		ArrayList pointerLocList = new ArrayList(); // the location of the pointer in the page
		
		// Find the right leaf node to insert the key
		// The pointer doesn't point to leaf node if it is less than MAX_INTERNAL_NODE_NUM, which can check
		// if we reach the leaf node
		while (currentPageId < MAX_INTERNAL_NODE_NUM) {
			InternalNode currentNode = internalNodes[currentPageId];
			long[] keys = currentNode.getKeys();
			int check = 0;
			for (int i = 0; i < currentNode.getCurrentLoc(); i++) {
				if (key < keys[i]) {
					pointerLoc = i;
					check = 1;
					break;
				}
			}
			// If the key greater than all keys in the node, use the right end pointer 
			// to go to the next level of the tree
			if (check == 0) {
				pointerLoc = currentNode.getCurrentLoc();
			}
			// Record the the current page id and the location of the matched pointer
			pageIdList.add(currentPageId);
			pointerLocList.add(pointerLoc);
			
			// Get the next page id
			currentPageId = currentNode.getChildPageId()[pointerLoc];
		}
		
		// Insert the key into the leaf node
		LeafNode currentLeafNode = leafNodes[currentPageId - MAX_INTERNAL_NODE_NUM];
		if (!currentLeafNode.isFull()) {
			currentLeafNode.insertDataEntry(key, heapPageId, heapRecordId);
			currentLeafNode.next();
		}
		else {
			// Split the leaf node into two leaf node
			currentLeafNode.insertDataEntry(key, heapPageId, heapRecordId);
			long[] keys = currentLeafNode.getKeys();
			int[] heapPageIds = currentLeafNode.getHeapPageIds();
			int[] heapRecordIds = currentLeafNode.getHeapRecordIds();
			long[] temp1 = new long[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
			int[] temp2 = new int[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
			int[] temp3 = new int[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
			Arrays.fill(temp1, -1);
			Arrays.fill(temp2, -1);
			Arrays.fill(temp3, -1);
			int firstHalfLength = (int) Math.floor((MAX_DATA_ENTRY_IN_LEAF_NODE + 1) / 2.0);
			int secondHalfLength = keys.length - firstHalfLength;
			System.arraycopy(keys, 0, temp1, 0, firstHalfLength);
			System.arraycopy(heapPageIds, 0, temp2, 0, firstHalfLength);
			System.arraycopy(heapRecordIds, 0, temp3, 0, firstHalfLength);
			currentLeafNode.setKeys(temp1);
			currentLeafNode.setHeapPageIds(temp2);
			currentLeafNode.setHeapRecordIds(temp3);
			currentLeafNode.setCurrentLoc(firstHalfLength);
			currentLeafNode.setFull(false);
			LeafNode newLeafNode = new LeafNode(MAX_DATA_ENTRY_IN_LEAF_NODE);
			temp1 = new long[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
			temp2 = new int[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
			temp3 = new int[MAX_DATA_ENTRY_IN_LEAF_NODE + 1];
			Arrays.fill(temp1, -1);
			Arrays.fill(temp2, -1);
			Arrays.fill(temp3, -1);
			System.arraycopy(keys, firstHalfLength, temp1, 0, secondHalfLength);
			System.arraycopy(heapPageIds, firstHalfLength, temp2, 0, secondHalfLength);
			System.arraycopy(heapRecordIds, firstHalfLength, temp3, 0, secondHalfLength);
			newLeafNode.setKeys(temp1);
			newLeafNode.setHeapPageIds(temp2);
			newLeafNode.setHeapRecordIds(temp3);
			newLeafNode.setCurrentLoc(secondHalfLength);

			// Change the pointer between the leaf nodes
			if (currentLeafNode.getRightPageId() != -1) {
				int rightId = currentLeafNode.getRightPageId();
				leafNodes[rightId - MAX_INTERNAL_NODE_NUM].setLeftPageId(currentLocForL + MAX_INTERNAL_NODE_NUM);
				newLeafNode.setRightPageId(rightId);
			}
			currentLeafNode.setRightPageId(currentLocForL + MAX_INTERNAL_NODE_NUM);
			newLeafNode.setLeftPageId(currentPageId);
			
			// Get the new key and page id for sending them to the last level
			long newKey = newLeafNode.getKeys()[0];
			int newPageId = currentLocForL + MAX_INTERNAL_NODE_NUM;
			
			// Save the new leaf node to the leafNodes array
			leafNodes[currentLocForL] = newLeafNode;
			currentLocForL++;

			// Send the new key and the page id of the nodes created by splitting to the last level
			int currInterPageId = (int) pageIdList.get(pageIdList.size() - 1);
			int currInterPointerLoc = (int) pointerLocList.get(pointerLocList.size() - 1);
			InternalNode currentIntN = internalNodes[currInterPageId];
			int count = 2;
			boolean isRoot = false;
			while (currentIntN.isFull()) {
				currentIntN.insertKeyAndChildPointer(newKey, currInterPointerLoc, newPageId);
				keys = currentIntN.getKeys();
				int[] pointers = currentIntN.getChildPageId();
				temp1 = new long[MAX_CHILD_NODES_IN_INTERNAL_NODE];
				temp2 = new int[MAX_CHILD_NODES_IN_INTERNAL_NODE + 1];
				Arrays.fill(temp1, -1);
				Arrays.fill(temp2, -1);
				firstHalfLength = (int) Math.floor((MAX_CHILD_NODES_IN_INTERNAL_NODE + 1) / 2.0);
				secondHalfLength = pointers.length - firstHalfLength;
				System.arraycopy(keys, 0, temp1, 0, firstHalfLength - 1);
				System.arraycopy(pointers, 0, temp2, 0, firstHalfLength);
				currentIntN.setKeys(temp1);
				currentIntN.setChildPageId(temp2);
				currentIntN.setCurrentLoc(firstHalfLength - 1);
				currentIntN.setFull(false);
				InternalNode newIntNode = new InternalNode();
				temp1 = new long[MAX_CHILD_NODES_IN_INTERNAL_NODE];
				temp2 = new int[MAX_CHILD_NODES_IN_INTERNAL_NODE + 1];
				Arrays.fill(temp1, -1);
				Arrays.fill(temp2, -1);
				System.arraycopy(keys, firstHalfLength, temp1, 0, secondHalfLength - 1);
				System.arraycopy(pointers, firstHalfLength, temp2, 0, secondHalfLength);
				newIntNode.setKeys(temp1);
				newIntNode.setChildPageId(temp2);
				newIntNode.setCurrentLoc(secondHalfLength - 1);
				
				// Save the new internal node to the internalNodes array
				internalNodes[currentLocForI] = newIntNode;
				currentLocForI++;
				
				// If the current internal node is the root node, split it into two nodes and create new 
				// root node to connect these two nodes
				if (currInterPageId == rootNodeLoc) {
					InternalNode rootNode = new InternalNode(MAX_CHILD_NODES_IN_INTERNAL_NODE,
							keys[firstHalfLength - 1], currInterPageId, currentLocForI - 1);
					rootNodeLoc = currentLocForI; // change the root node
					internalNodes[currentLocForI] = rootNode;
					currentLocForI++;
					isRoot = true;
					break;
				}
				
				newKey = newIntNode.getKeys()[0];
				newPageId = currentLocForL - 1;
				currInterPageId = (int) pageIdList.get(pageIdList.size() - count);
				currInterPointerLoc = (int) pointerLocList.get(pointerLocList.size() - count);
				currentIntN = internalNodes[currInterPageId];
				count++;
			}
			// If the current internal node isn't full or it is not a root node, just add the key and pointer to the internal node
			if (!isRoot) {
				currentIntN.insertKeyAndChildPointer(newKey, currInterPointerLoc, newPageId);
				currentIntN.next();
			}
		}
	}
	
	
	/**
	 * convert int to byte array
	 */
	public static byte[] intToBytes(int s) {
        byte[] array = new byte[4];

        for (int i = 0; i < array.length; i++) {
            array[array.length - 1 - i] = (byte) (s >> (i * 8));
        }
        return array;
    }
	
	/**
	 * convert byte array to long
	 */
	public static long bytesToLong(byte[] array) {
        long value = 0;
        for (int i = 0; i < array.length ; i++) {
            value |= ((long)(array[i] & 0xff) << ((array.length - i - 1) * 8));
        }
        return value;
    }

	/**
	 * convert long to byte array
	 */
	public static byte[] longToBytes(long l) {
        byte[] array = new byte[8];

        for (int i = 0; i < array.length; i++) {
            array[array.length - 1 - i] = (byte) (l >> (i * 8));
        }
        return array;
    }
	
	public static byte[] longArrayToBytes(long[] longArray) {
		byte[] array = new byte[8 * longArray.length];
		int count = 0;
		for (int i = 0; i < longArray.length; i++) {
			byte[] temp = longToBytes(longArray[i]);
			System.arraycopy(temp, 0, array, count, temp.length);
			count += 8;
		}
		return array;
	}
	
	public static byte[] intArrayToBytes(int[] intArray) {
		byte[] array = new byte[4 * intArray.length];
		int count = 0;
		for (int i = 0; i < intArray.length; i++) {
			byte[] temp = intToBytes(intArray[i]);
			System.arraycopy(temp, 0, array, count, temp.length);
			count += 4;
		}
		return array;
	}
}
