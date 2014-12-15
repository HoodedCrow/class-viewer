package classviewer.filters;

import java.util.ArrayList;
import java.util.HashSet;

import classviewer.CourseFilterFrame;
import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.Source;

/**
 * Course filter by course status (unknown/done/etc).
 * 
 * @author TK
 */
public class SourceCourseFilter extends CourseFilter {
	private ArrayList<Source> options = null;
	private HashSet<Source> selected = new HashSet<Source>();
	private CourseFilterFrame parentFrame;

	public SourceCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By source";
	}

	@Override
	public ArrayList<? extends Object> getOptions() {
		if (options == null) {
			options = new ArrayList<Source>();
			for (Source s : Source.values())
				options.add(s);
		}
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
		parentFrame.sourceSelectionChanged(this.selected);
	}

	@Override
	public void setActive(boolean value) {
		super.setActive(value);
		parentFrame.sourceSelectionChanged(value ? this.selected : null);
	}

	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active)
			return true;
		return selected.contains(rec.getSource());
	}

	public void setParentFrame(CourseFilterFrame parentFrame) {
		this.parentFrame = parentFrame;
	}

	@Override
	public ArrayList<? extends Object> getVisibleOptions(
			HashSet<Source> selectedSources) {
		return getOptions();
	}
}
