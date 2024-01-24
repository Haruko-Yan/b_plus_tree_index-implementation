import java.util.Arrays;

public class LeafNode {
	
	private int leftPageId = -1;
	private int rightPageId = -1;
	private long[] keys;
	private int[] heapRecordIds;
	private int[] heapPageIds;
	private int currentLoc = 0; // the pointer that records the first position which can be used
	private boolean isFull = false; // a flag that check if the leaf node is full
	

	public LeafNode(int dataEntryNum) {
		keys = new long[dataEntryNum + 1];
		heapRecordIds = new int[dataEntryNum + 1];
		heapPageIds = new int[dataEntryNum + 1];
		Arrays.fill(keys, -1);
		Arrays.fill(heapRecordIds, -1);
		Arrays.fill(heapPageIds, -1);
	}
	
	
	public int[] getHeapPageIds() {
		return heapPageIds;
	}
	
	public void setHeapPageIds(int[] heapPageIds) {
		this.heapPageIds = heapPageIds;
	}
	
	public int[] getHeapRecordIds() {
		return heapRecordIds;
	}
	
	public void setHeapRecordIds(int[] heapRecordIds) {
		this.heapRecordIds = heapRecordIds;
	}

	public int getLeftPageId() {
		return leftPageId;
	}


	public void setLeftPageId(int leftPageId) {
		this.leftPageId = leftPageId;
	}


	public int getRightPageId() {
		return rightPageId;
	}


	public void setRightPageId(int rightPageId) {
		this.rightPageId = rightPageId;
	}
	
	public int getCurrentLoc() {
		return currentLoc;
	}
	
	public void setCurrentLoc(int currentLoc) {
		this.currentLoc = currentLoc;
	}
	
	public long[] getKeys() {
		return keys;
	}
	
	public void setKeys(long[] keys) {
		this.keys = keys;
	}
	
	public boolean isFull() {
		return isFull;
	}

	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}
	
	/**
	 * insert the data entry in order
	 */
	public void insertDataEntry(long key, int pageId, int recordId) {
		int index = indexForSort(key);
		for (int i = currentLoc; i > index; i--) {
			keys[i] = keys[i - 1];
			heapRecordIds[i] = heapRecordIds[i - 1];
			heapPageIds[i] = heapPageIds[i - 1];
		}
		keys[index] = key;
		heapRecordIds[index] = recordId;
		heapPageIds[index] = pageId;
	}

	
	/**
	 * Find the right position of the key in keys array in order to make them sorted
	 */
	public int indexForSort(long key) {
		for (int i = 0; i < currentLoc; i++) {
			if (key < keys[i]) return i;
		}
		return currentLoc;
	}
	
	
	/**
	 * Move the pointer to the next position which is not being used. If the node is full, turn flag to true
	 */
	public void next() {
		if (currentLoc < keys.length - 2)
			currentLoc++;
		else {
			isFull = true;
			currentLoc++;
		}	
	}




}
