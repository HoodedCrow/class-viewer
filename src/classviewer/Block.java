package classviewer;

import classviewer.model.OffRec;

/** Block for a class offering */
public class Block {
	/** First and last week of the offering */
	int start, len;
	/** Row */
	int row = -1;
	OffRec offering;

	public Block(int start, int len, OffRec or) {
		this.start = start;
		this.len = len;
		this.offering = or;
	}

	public String getLabel() {
		return offering.getCourse().getStatus().toString()
				+ offering.getCourse().getOfferings().size() + " "
				+ offering.getCourse().getName() + ", "
				+ offering.getStartStr() + ", " + offering.getDurStr();
	}

	@Override
	public String toString() {
		return offering.toString();
	}
}