package classviewer;

import java.util.ArrayList;

public interface BlockFilter {
	/** Prune the list of blocks in place and return the same list for chaining */
	public ArrayList<Block> filter(ArrayList<Block> list);

	/** Enable/disable. Disabled filter passes blocks through */
	public void setEnabled(boolean value);
}
