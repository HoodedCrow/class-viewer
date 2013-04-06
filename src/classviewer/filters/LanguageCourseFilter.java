package classviewer.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;

/**
 * Course filter by language. All languages are picked from the model.
 * 
 * @author TK
 */
public class LanguageCourseFilter extends CourseFilter {
	private HashSet<String> selected = new HashSet<String>();

	public LanguageCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By language";
	}

	@Override
	public Collection<? extends Object> getOptions() {
		ArrayList<String> options = new ArrayList<String>(model.getLanguages());
		Collections.sort(options);
		return options;
	}

	@Override
	public String getDescription(Object option) {
		return (String) option;
	}

	@Override
	public boolean isSelected(Object option) {
		return selected.contains(option);
	}

	@Override
	public void setSelected(Object option, boolean selected) {
		if (selected)
			this.selected.add((String) option);
		else
			this.selected.remove(option);
		model.fireFiltersChanged(this);
	}

	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active)
			return true;
		return selected.contains(rec.getLanguage());
	}
}
