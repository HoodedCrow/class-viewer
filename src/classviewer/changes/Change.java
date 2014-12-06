package classviewer.changes;

import classviewer.model.CourseModel;
import classviewer.model.Source;

/**
 * Parent class for changes to descriptions, courses, and offerings.
 * 
 * @author TK
 */
public abstract class Change implements Comparable<Change> {
	public static final String ADD = "+";
	public static final String DELETE = "-";
	public static final String MODIFY = "*";

	protected Source source;
	protected String type;
	protected int order;

	public Change(Source source, String type) {
		this.source = source;
		this.type = type;
	}

	public Source getSource() {
		return source;
	}
	
	public String getType() {
		return type;
	}

	public abstract String getDescription();

	public abstract Object getTarget();

	public abstract Object getNewValue();

	public abstract Object getOldValue();

	public abstract void apply(CourseModel model);

	public int getOrder() {
		return order;
	}

	@Override
	public int compareTo(Change other) {
		// Add category/uni first
		// Add class
		// Add offering
		// Change anything
		// Delete offering
		// Delete class
		// Delete category/uni
		return Integer.compare(this.getOrder(), other.getOrder());
	}
}
