package classviewer.filters;

import java.util.Collection;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;

public abstract class CourseFilter {

	protected boolean active = false;

	protected CourseModel model;

	public abstract String getName();

	public CourseFilter(CourseModel model) {
		this.model = model;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean value) {
		this.active = value;
		model.fireFiltersChanged(this);
	}

	public abstract Collection<? extends Object> getOptions();

	public abstract String getDescription(Object option);

	public abstract boolean isSelected(Object option);

	public abstract void setSelected(Object option, boolean selected);

	public abstract boolean accept(CourseRec rec);
}
