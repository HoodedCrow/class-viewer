/** Common parent for university and category filters */
package classviewer.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.DescRec;
import classviewer.model.Source;

public abstract class DescCourseFilter extends CourseFilter {
	protected HashSet<DescRec> selected = new HashSet<DescRec>();
	protected Collection<Source> cachedForSources = null;
	protected ArrayList<DescRec> cachedSelection = new ArrayList<DescRec>();
	protected ArrayList<DescRec> options = new ArrayList<DescRec>();

	public DescCourseFilter(CourseModel courseModel) {
		super(courseModel);
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
	public ArrayList<? extends Object> getVisibleOptions(
			HashSet<Source> selectedSources) {
		if (cachedForSources == null && selectedSources == null
				|| cachedForSources != null
				&& cachedForSources.equals(selectedSources))
			return cachedSelection;
		if (options.isEmpty())
			getOptions(); // Prepopulate
		cachedSelection.clear();
		if (selectedSources == null) {
			cachedSelection.addAll(options);
		} else {
			for (DescRec rec : options)
				if (selectedSources.contains(rec.getSource()))
					cachedSelection.add(rec);
		}
		if (selectedSources == null) {
			cachedForSources = null;
		} else {
			cachedForSources = new HashSet<Source>(selectedSources);
		}
		return cachedSelection;
	}
}
