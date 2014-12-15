package classviewer.filters;

import java.util.ArrayList;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.Source;

public abstract class SimpleCourseFilter<T extends Object> extends CourseFilter {
	protected HashSet<T> selected = new HashSet<T>();
	protected ArrayList<T> options = null;

	public SimpleCourseFilter(CourseModel model) {
		super(model);
	}

	@Override
	public String getDescription(Object option) {
		return option.toString();
	}

	@Override
	public boolean isSelected(Object option) {
		return selected.contains(option);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setSelected(Object option, boolean selected) {
		if (selected)
			this.selected.add((T) option);
		else
			this.selected.remove(option);
		model.fireFiltersChanged(this);
	}

	@Override
	public ArrayList<? extends Object> getVisibleOptions(
			HashSet<Source> selectedSources) {
		return getOptions();
	}
}
