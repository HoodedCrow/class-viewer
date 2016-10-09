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
	protected String toolTip = null;
	protected Object object;

	public Change(Source source, String type, Object object) {
		this.source = source;
		this.type = type;
		this.object = object;
	}

	public Source getSource() {
		return source;
	}
	
	public String getType() {
		return type;
	}
	
	public Object getObject() {
		return object;
	}
	
	public Change setOrder(int order) {
		this.order = order;
		return this;
	}
	
	public Change setToolTop(String toolTip) {
		this.toolTip = toolTip;
		return this;
	}

	public abstract String getDescription();

	public abstract Object getTarget();

	public abstract Object getNewValue();

	public abstract Object getOldValue();

	public abstract void apply(CourseModel model);

	public int getOrder() {
		return order;
	}
	
	public boolean isActivation() {
		return false;
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

	public static boolean fieldChanged(String existing, String incoming) {
		if (existing != null && existing.isEmpty())
			existing = null;
		if (incoming != null && incoming.isEmpty())
			incoming = null;
		return (existing == null && incoming != null || existing != null
				&& !existing.equals(incoming));
	}

	public String getToolTip() {
		return toolTip;
	}
}
