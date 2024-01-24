import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class btsearch {

	static int MAX_DATA_ENTRY_IN_LEAF_NODE;
	static int MAX_CHILD_NODES_IN_INTERNAL_NODE;
	
	public static void main(String[] args) throws ParseException {
		// check for correct number of arguments
        if (args.length != 4) {
            System.out.println("Error: Incorrect number of arguments were input");
            return;
        }
        
        // Get the start date and end date from the command line
     	SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
     	long startMillis = ft.parse(args[2]).getTime(); // milliseconds of the start date
     	long endMillis = ft.parse(args[3]).getTime(); // milliseconds of the end date
     	
     	FileInputStream inHeap = null;
     	FileInputStream inIndex = null;
     	
     	int recordSize = 1592;
     	int pageSize = Integer.parseInt(args[0].split("\\.")[1]);
     	MAX_DATA_ENTRY_IN_LEAF_NODE = (int) Math.floor((pageSize - 9) / 16.0);
		MAX_CHILD_NODES_IN_INTERNAL_NODE = (int) Math.floor((pageSize + 7) / 12.0);
     	
     	
     	try {
			inHeap = new FileInputStream(args[0]);
			inIndex = new FileInputStream(args[1]);
			byte[] page = new byte[pageSize];
			
			long startTime = System.currentTimeMillis();
			// Get the location of the root node
			byte[] rootNodeLoc = new byte[4];
			inIndex.read(rootNodeLoc);
			inIndex.close();
			int pageId = bytesToInt(rootNodeLoc);
			
			while (true) {
				inIndex = new FileInputStream(args[1]);
				inIndex.skip(4 + pageId * pageSize);
				inIndex.read(page);
				inIndex.close();
				// If the node is leaf node, break the loop
				if (page[0] == 1) {
					break;
				}
				byte[] keysByte = new byte[8 * (MAX_CHILD_NODES_IN_INTERNAL_NODE - 1)];
				System.arraycopy(page, 1, keysByte, 0, keysByte.length);
				long[] keys = bytesToLongArray(keysByte);
				int pointerLoc = 0;
				int check = 0;
				for (int i = 0; i < keys.length; i++) {
					// When read a null value, the date is greater than all date, so use the right end pointer
					if (keys[i] == -1) {
						pointerLoc = i;
						break;
					}
					if (startMillis < keys[i]) {
						pointerLoc = i;
						check = 1;
						break;
					}
				}
				byte[] pointersByte = new byte[4 * MAX_CHILD_NODES_IN_INTERNAL_NODE];
				System.arraycopy(page, 1 + keysByte.length, pointersByte, 0, pointersByte.length);
				int[] pointers = bytesToIntArray(pointersByte);
				pageId = pointers[pointerLoc]; // get the next page id
			}
			
			// Extract the keys, page id and record id
			byte[] keysByte = new byte[8 * MAX_DATA_ENTRY_IN_LEAF_NODE];
			System.arraycopy(page, 9, keysByte, 0, keysByte.length);
			long[] keys = bytesToLongArray(keysByte);
			
			byte[] pageIdsByte = new byte[4 * MAX_DATA_ENTRY_IN_LEAF_NODE];
			System.arraycopy(page, 9 + keysByte.length, pageIdsByte, 0, pageIdsByte.length);
			int[] pageIds = bytesToIntArray(pageIdsByte);
			
			byte[] recordIdsByte = new byte[4 * MAX_DATA_ENTRY_IN_LEAF_NODE];
			System.arraycopy(page, 9 + keysByte.length + pageIdsByte.length, recordIdsByte, 0, recordIdsByte.length);
			int[] recordIds = bytesToIntArray(recordIdsByte);

			// Compare the date with the key of the leaf node
			byte[] record = new byte[recordSize];
			for (int i = 0; i < keys.length; i++) {
				if (keys[i] == -1) {
					break;
				}
				if (keys[i] >= startMillis) {
					inHeap = new FileInputStream(args[0]);
					inHeap.skip(pageIds[i] * pageSize + recordIds[i] * recordSize);
					inHeap.read(record);
					inHeap.close();
					parseRecord(record);
				}
			}
			
			int check = 0;
			do {
				byte[] rightPageIdBytes = new byte[4];
				System.arraycopy(page, 5, rightPageIdBytes, 0, rightPageIdBytes.length);
				int rightPageId = bytesToInt(rightPageIdBytes);
				inIndex = new FileInputStream(args[1]);
				inIndex.skip(4 + rightPageId * pageSize);
				inIndex.read(page);
				inIndex.close();
				// Extract the keys, page id and record id
				keysByte = new byte[8 * MAX_DATA_ENTRY_IN_LEAF_NODE];
				System.arraycopy(page, 9, keysByte, 0, keysByte.length);
				keys = bytesToLongArray(keysByte);
				
				pageIdsByte = new byte[4 * MAX_DATA_ENTRY_IN_LEAF_NODE];
				System.arraycopy(page, 9 + keysByte.length, pageIdsByte, 0, pageIdsByte.length);
				pageIds = bytesToIntArray(pageIdsByte);
				
				recordIdsByte = new byte[4 * MAX_DATA_ENTRY_IN_LEAF_NODE];
				System.arraycopy(page, 9 + keysByte.length + pageIdsByte.length, recordIdsByte, 0, recordIdsByte.length);
				recordIds = bytesToIntArray(recordIdsByte);

				// Compare the date with the key of the leaf node
				record = new byte[recordSize];
				for (int i = 0; i < keys.length; i++) {
					if (keys[i] == -1) {
						break;
					}
					if (keys[i] <= endMillis) {
						inHeap = new FileInputStream(args[0]);
						inHeap.skip(pageIds[i] * pageSize + recordIds[i] * recordSize);
						inHeap.read(record);
						inHeap.close();
						parseRecord(record);
					}
					else {
						check = 1;
					}
				}
			} while (check == 0);
			
			long endTime = System.currentTimeMillis();
			System.out.println();
			System.out.println("The number of milliseconds to do this query:" + (endTime - startTime) +
					" milliseconds");
			
		} catch (FileNotFoundException e) {
			System.err.println("Error: The file is not found!" + e.getMessage());
		} catch (IOException e) {
			System.err.println("Error: IOException" + e.getMessage());
		}
	}
	
	
	public static void parseRecord(byte[] record) {
		// personName
		int counter = 0; // for counting the length of non-null bytes
		for (int i = 0; i < 60; i++) {
			if (record[i] == 0) {
				break;
			}
			counter++;
		}
		// If it's all 0 values of the bytes in this field, print 'NULL' to stdout
		if (counter == 0) {
			System.out.print("NULL" + "\t");
		}
		else {
			// Extract the String of personName
			String personName = new String(record, 0, counter);
			System.out.print(personName + "\t");
		}
		
		// birthDate
		byte[] birthDateBytes = new byte[8];
		System.arraycopy(record, 60, birthDateBytes, 0, birthDateBytes.length);
		long birthDate = bytesToLong(birthDateBytes);
		SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd");
		System.out.print(sFormat.format(new Date(birthDate)) + "\t");
		
		// birthPlace label
		counter = 0; // initialize the counter
		for (int i = 0; i < 150; i++) {
			if (record[i + 76] == 0) {
				break;
			}
			counter++;
		}
		// If it's all 0 values of the bytes in this field, print 'NULL' to stdout
		if (counter == 0) {
			System.out.print("NULL" + "\t");
		}
		else {
			// Extract the String of birthPlace
			String birthPlace = new String(record, 76, counter);
			System.out.print(birthPlace + "\t");
		}
		
		// deathDate (same as birthDate)
		// birthDate
		byte[] deathDateBytes = new byte[8];
		System.arraycopy(record, 60, deathDateBytes, 0, deathDateBytes.length);
		long deathDate = bytesToLong(deathDateBytes);
		if (deathDate == -1) {
			System.out.print("NULL" + "\t");
		}
		else {
			System.out.print(sFormat.format(new Date(deathDate)) + "\t");
		}
		
		
		// Print the next 5 fields to stdout
		int[] position = {242, 442, 672, 928, 978, 1253}; // the location of the next six fields in the record
		for (int i = 0; i < position.length - 1; i++) {
			counter = 0; // initialize the counter
			for (int j = 0; j < position[i + 1] - position[i]; j++) {
				if (record[j + position[i]] == 0) {
					break;
				}
				counter++;
			}
			// If it's all 0 values of the bytes in this field, print 'NULL' to stdout
			if (counter == 0) {
				System.out.print("NULL" + "\t");
			}
			else {
				// Extract the String of birthPlace
				String field = new String(record, position[i], counter);
				System.out.print(field + "\t");
			}
		}
		
		// wikiPageID
		byte[] wikiPageID = new byte[4];
		for (int i = 0; i < wikiPageID.length; i++) {
			wikiPageID[i] = record[i + 1253];
		}
		System.out.print(bytesToInt(wikiPageID) + "\t");
		
		// description
		counter = 0; // initialize the counter
		for (int i = 0; i < 335; i++) {
			if (record[i + 1257] == 0) {
				break;
			}
			counter++;
		}
		// If it's all 0 values of the bytes in this field, print 'NULL' to stdout
		if (counter == 0) {
			System.out.print("NULL" + "\t");
		}
		else {
			// Extract the String of birthPlace
			String birthPlace = new String(record, 1257, counter);
			System.out.println(birthPlace);
		}
		
	}
	
	
	/**
	 * convert byte array to int
	 */
	public static int bytesToInt(byte[] array) {
        int value = 0;
        for (int i = 0; i < array.length ; i++) {
            value |= ((array[i] & 0xff) << ((array.length - i - 1) * 8));
        }
        return value;
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
	
	public static long[] bytesToLongArray(byte[] array) {
		long[] longArray = new long[array.length / 8];
		for (int i = 0; i < longArray.length; i++) {
			byte[] temp = new byte[8];
			System.arraycopy(array, 8 * i, temp, 0, temp.length);
			longArray[i] = bytesToLong(temp);
		}
		return longArray;
	}
	
	public static int[] bytesToIntArray(byte[] array) {
		int[] intArray = new int[array.length / 4];
		for (int i = 0; i < intArray.length; i++) {
			byte[] temp = new byte[4];
			System.arraycopy(array, 4 * i, temp, 0, temp.length);
			intArray[i] = bytesToInt(temp);
		}
		return intArray;
	}

}
