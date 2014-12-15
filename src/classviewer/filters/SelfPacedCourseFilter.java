package classviewer.filters;

import java.util.ArrayList;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public class SelfPacedCourseFilter extends SimpleCourseFilter<String> {
	private final String SELF_PACED = "Self-paced";
	private final String ONLY_SCHEDULED = "Only scheduled";

	public SelfPacedCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "Self-paced";
	}

	@Override
	public ArrayList<? extends Object> getOptions() {
		if (options == null) {
			options = new ArrayList<String>();
			options.add(SELF_PACED);
			options.add(ONLY_SCHEDULED);
		}
		return options;
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
