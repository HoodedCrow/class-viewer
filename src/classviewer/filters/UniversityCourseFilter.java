package classviewer.filters;

import java.util.ArrayList;
import java.util.Collections;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;
import classviewer.model.Source;

public class UniversityCourseFilter extends DescCourseFilter {
	public UniversityCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By university";
	}

	@Override
	public ArrayList<? extends Object> getOptions() {
		// All options, rebuild.
		options.clear();
		for (Source source : Source.values())
			options.addAll(model.getUniversities(source));
		Collections.sort(options);
		// Prepopulate cache as well.
		cachedSelection.clear();
		cachedForSources = null;
		cachedSelection.addAll(options);
		return options;
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
