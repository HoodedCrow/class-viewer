package classviewer.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;
import classviewer.model.Source;

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
		ArrayList<DescRec> options = new ArrayList<DescRec>();
		for (Source source : Source.values())
			options.addAll(model.getUniversities(source));
		Collections.sort(options);
		return options;
	}

	@Override
	public String getDescription(Object option) {
		DescRec o = (DescRec) option;
		return o.getName() + "(" + o.getSource().oneLetter() + ")";
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
