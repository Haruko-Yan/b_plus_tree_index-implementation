import java.util.Arrays;

public class InternalNode {
	
	private int[] childPageId; // pointer to child node
	private long[] keys;
	private int currentLoc = 0; // the pointer that records the first position which can be used for the keys array
	private boolean isFull = false; // a flag that check if the internal node is full
	

	/**
	 * After initialize the internal node, insert the first key and two pointer to both side of the key
	 * @param childNum
	 * @param key
	 * @param leftPointer
	 * @param rightPointer
	 */
	public InternalNode(int childNum, long key, int leftPointer, int rightPointer) {
		keys = new long[childNum];
		childPageId = new int[childNum + 1];
		Arrays.fill(keys, -1);
		Arrays.fill(childPageId, -1);
		keys[0] = key;
		childPageId[0] = leftPointer;
		childPageId[1] = rightPointer;
		currentLoc = 1;
	}
	
	public InternalNode() {}

	public boolean isFull() {
		return isFull;
	}
	
	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}
	
	public long[] getKeys() {
		return keys;
	}
	
	public void setKeys(long[] keys) {
		this.keys = keys;
	}
	
	
	public int[] getChildPageId() {
		return childPageId;
	}
	
	public void setChildPageId(int[] childPageId) {
		this.childPageId = childPageId;
	}
	
	public int getCurrentLoc() {
		return currentLoc;
	}
	
	public void setCurrentLoc(int currentLoc) {
		this.currentLoc = currentLoc;
	}
	
	
	/**
	 * insert the key and pointer pointing to child node in order
	 * @param key
	 * @param pointerLoc 
	 * 	the location of the previous pointer pointing to the full node
	 * @param newChildPageId
	 * 	the new pointer pointing to the newly created child node 
	 */
	public void insertKeyAndChildPointer(long key, int pointerLoc, int newChildPageId) {
		for (int i = currentLoc; i > pointerLoc; i--) {
			childPageId[i + 1] = childPageId[i];
			keys[i] = keys[i - 1];
		}
		keys[pointerLoc] = key;
		childPageId[pointerLoc + 1] = newChildPageId;
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
