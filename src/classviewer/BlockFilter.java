package classviewer;

import java.util.ArrayList;

import classviewer.model.Status;

/**
 * Remove blocks with the given Status.
 * 
 * @author TK
 */
public class BlockFilter {
	private Status status;
	private boolean enabled = false;

	public BlockFilter(Status status) {
		this.status = status;
	}

	/** Prune the list of blocks in place and return the same list for chaining */
	public ArrayList<Block> filter(ArrayList<Block> list) {
		if (!enabled)
			return list;
		for (int i = 0; i < list.size(); i++)
			// Okay to check status by pointer
			if (list.get(i).offering.getStatus() == status)
				list.remove(i--);
		return list;
	}

	/** Enable/disable. Disabled filter passes blocks through */
	public void setEnabled(boolean value) {
		this.enabled = value;
	}

	public Status getStatus() {
		return status;
	}
}
