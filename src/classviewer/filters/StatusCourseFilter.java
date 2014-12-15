package classviewer.filters;

import java.util.ArrayList;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.Status;

/**
 * Course filter by course status (unknown/done/etc).
 * 
 * @author TK
 */
public class StatusCourseFilter extends SimpleCourseFilter<Status> {
	public StatusCourseFilter(CourseModel courseModel) {
		super(courseModel);
	}

	@Override
	public String getName() {
		return "By status";
	}

	@Override
	public String getDescription(Object option) {
		return ((Status) option).getName();
	}

	@Override
	public ArrayList<? extends Object> getOptions() {
		if (options == null) {
			options = new ArrayList<Status>();
			for (Status s : Status.getAll())
				options.add(s);
		}
		return options;
	}

	@Override
	public boolean accept(CourseRec rec) {
		if (!this.active)
			return true;
		return selected.contains(rec.getStatus());
	}
}
