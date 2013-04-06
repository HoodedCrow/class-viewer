package classviewer.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.Status;

/**
 * Course filter by course status (unknown/done/etc).
 * 
 * @author TK
 */
public class StatusCourseFilter extends CourseFilter {
	private HashSet<Status> selected = new HashSet<Status>();

	public StatusCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By status";
	}

	@Override
	public Collection<? extends Object> getOptions() {
		ArrayList<Status> options = new ArrayList<Status>();
		for (Status s : Status.getAll())
			options.add(s);
		return options;
	}

	@Override
	public String getDescription(Object option) {
		return ((Status) option).getName();
	}

	@Override
	public boolean isSelected(Object option) {
		return selected.contains(option);
	}

	@Override
	public void setSelected(Object option, boolean selected) {
		if (selected)
			this.selected.add((Status) option);
		else
			this.selected.remove(option);
		model.fireFiltersChanged(this);
	}

	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active)
			return true;
		return selected.contains(rec.getStatus());
	}
}
