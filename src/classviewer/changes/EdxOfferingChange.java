package classviewer.changes;

import java.util.Date;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public class EdxOfferingChange extends OfferingChange {

	private EdxRecord record;

	public EdxOfferingChange(String type, CourseRec course, String field,
			OffRec offering, EdxRecord record) {
		// Pass NULL for JSON
		super(type, course, field, offering, null);
		this.record = record;
		if (record != null)
			created = makeEdxOffering(record);
	}

	@Override
	protected Object getField(OffRec rec) {
		if ("Duration".equals(field))
			return rec.getDuration();
		return super.getField(rec);
	}

	@Override
	public Object getTarget() {
		if (offering != null)
			return offering.getCourse().getName() + " [" + offering.getId()
					+ "]";
		return course.getName() + " [" + record.getCourseId() + "]";
	}

	@Override
	public void apply(CourseModel model) {
		if (type == ADD) {
			created.updateId(model.getNewNegativeId());
		}
		super.apply(model);
	}

	public static OffRec makeEdxOffering(EdxRecord record) {
		Integer id = 0; // To be set during ADD commit

		// Some of these might be missing
		int spread = 1;
		Date start = record.getStart();
		String startStr = null;
		if (start != null)
			startStr = OffRec.dformat.format(start);

		String home = record.getHome();

		int duration = record.getDuration();
		Boolean active = false; // Set based on calendar?
		return new OffRec(id, start, duration, spread, home, active, startStr);
	}
}
