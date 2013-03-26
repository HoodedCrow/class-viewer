package classviewer.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;

public class UniversityCourseFilter extends CourseFilter {

	private HashSet<DescRec> selected = new HashSet<DescRec>();

	public UniversityCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By university";
	}

	@Override
	public Collection<? extends Object> getOptions() {
		ArrayList<DescRec> options = new ArrayList<DescRec>(model.getUniversities());
		Collections.sort(options);
		return options;
	}

	@Override
	public String getDescription(Object option) {
		return ((DescRec) option).getName();
	}

	@Override
	public boolean isSelected(Object option) {
		return selected.contains(option);
	}

	@Override
	public void setSelected(Object option, boolean selected) {
		if (selected)
			this.selected.add((DescRec) option);
		else
			this.selected.remove(option);
		model.fireFiltersChanged(this);
	}

	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active || rec.getUniversities().isEmpty())
			return true;
		for (DescRec r : rec.getUniversities())
			if (selected.contains(r))
				return true;
		return false;
	}
}
