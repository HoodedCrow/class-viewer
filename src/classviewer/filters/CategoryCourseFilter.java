package classviewer.filters;

import java.util.Collection;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.DescRec;

public class CategoryCourseFilter extends CourseFilter {

	private CourseModel model;
	private HashSet<DescRec> selected = new HashSet<DescRec>();

	public CategoryCourseFilter(CourseModel courseModel) {
		this.model = courseModel;
	}

	@Override
	public String getName() {
		return "By category";
	}

	@Override
	public Collection<? extends Object> getOptions() {
		return model.getCategories();
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
	}
}
