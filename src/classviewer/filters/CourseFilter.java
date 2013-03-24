package classviewer.filters;

import java.util.Collection;

public abstract class CourseFilter {

	protected boolean active = false;

	public abstract String getName();

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean value) {
		this.active = value;
	}

	public abstract Collection<? extends Object> getOptions();

	public abstract String getDescription(Object option);

	public abstract boolean isSelected(Object option);

	public abstract void setSelected(Object option, boolean selected);
}
