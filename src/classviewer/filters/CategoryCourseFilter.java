package classviewer.filters;

import java.util.ArrayList;
import java.util.Collections;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;
import classviewer.model.Source;

public class CategoryCourseFilter extends DescCourseFilter {
	public CategoryCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By category";
	}

	@Override
	public ArrayList<? extends Object> getOptions() {
		// All options, rebuild.
		options.clear();
		for (Source source : Source.values())
			options.addAll(model.getCategories(source));
		Collections.sort(options);
		// Prepopulate cache as well.
		cachedSelection.clear();
		cachedForSources = null;
		cachedSelection.addAll(options);
		return options;
	}
	
	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active || rec.getCategories().isEmpty())
			return true;
		for (DescRec r : rec.getCategories())
			if (selected.contains(r))
				return true;
		return false;
	}
}
