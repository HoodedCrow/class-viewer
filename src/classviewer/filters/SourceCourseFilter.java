package classviewer.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.Source;

/**
 * Course filter by course status (unknown/done/etc).
 * 
 * @author TK
 */
public class SourceCourseFilter extends CourseFilter {
	private HashSet<Source> selected = new HashSet<Source>();

	public SourceCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By source";
	}

	@Override
	public Collection<? extends Object> getOptions() {
		ArrayList<Source> options = new ArrayList<Source>();
		for (Source s : Source.values())
			options.add(s);
		return options;
	}

	@Override
	public String getDescription(Object option) {
		return ((Source) option).pretty();
	}

	@Override
	public boolean isSelected(Object option) {
		return selected.contains(option);
	}

	@Override
	public void setSelected(Object option, boolean selected) {
		if (selected)
			this.selected.add((Source) option);
		else
			this.selected.remove(option);
		model.fireFiltersChanged(this);
	}

	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active)
			return true;
		return selected.contains(rec.getSource());
	}
}
