package classviewer.changes;

import classviewer.model.CourseModel;

/**
 * Parent class for changes to descriptions, courses, and offerings.
 * 
 * @author TK
 */
public abstract class Change {
	public static final String ADD = "+";
	public static final String DELETE = "-";
	public static final String MODIFY = "*";

	protected String type;
	protected int order;

	public Change(String type) {
		this.type = type;
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
}
