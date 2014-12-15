package classviewer.filters;

import java.util.ArrayList;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.Source;

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

	public abstract ArrayList<? extends Object> getOptions();

	public abstract String getDescription(Object option);

	public abstract boolean isSelected(Object option);

	public abstract void setSelected(Object option, boolean selected);

	public abstract boolean accept(CourseRec rec);

	public abstract ArrayList<? extends Object> getVisibleOptions(
			HashSet<Source> selectedSources);
}
