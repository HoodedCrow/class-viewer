package classviewer.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.Source;

/**
 * Course filter by language. All languages are picked from the model.
 * 
 * @author TK
 */
public class LanguageCourseFilter extends SimpleCourseFilter<String> {
	public LanguageCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By language";
	}

	/**
	 * Always reload language here, and have a separate check in
	 * getVisibleOptions, because languages can change when model is reloaded
	 */
	@Override
	public ArrayList<? extends Object> getOptions() {
		options = new ArrayList<String>(model.getLanguages());
		Collections.sort(options);
		return options;
	}

	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active)
			return true;
		return selected.contains(rec.getLanguage());
	}

	@Override
	public ArrayList<? extends Object> getVisibleOptions(
			HashSet<Source> selectedSources) {
		if (options == null)
			getOptions();
		return options;
	}
}
