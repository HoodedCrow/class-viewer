package classviewer.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public class SelfPacedCourseFilter extends CourseFilter {
	private final String SELF_PACED = "Self-paced";
	private final String ONLY_SCHEDULED = "Only scheduled";
	private HashSet<String> selected = new HashSet<String>();

	public SelfPacedCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "Self-paced";
	}

	@Override
	public Collection<? extends Object> getOptions() {
		ArrayList<String> options = new ArrayList<String>();
		options.add(SELF_PACED);
		options.add(ONLY_SCHEDULED);
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
		if (selected) {
			this.selected.clear();
			this.selected.add((String) option);
		} else
			this.selected.remove(option);
		model.fireFiltersChanged(this);
	}

	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active)
			return true;
		// Has at least one self-paced offering.
		boolean selfPaced = false;
		for (OffRec ofr : rec.getOfferings())
			selfPaced = selfPaced || ofr.isSelfPaced();
		if (selfPaced && selected.contains(SELF_PACED))
			return true;
		if (!selfPaced && selected.contains(ONLY_SCHEDULED))
			return true;
		return false;
	}
}
