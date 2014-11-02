package classviewer.changes;

import java.util.Date;
import java.util.HashMap;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public class EdxOfferingChange extends OfferingChange {

	private Object newValue;

	public EdxOfferingChange(String type, CourseRec course, String field,
			OffRec offering, Object newValue) {
		// Pass NULL for JSON
		super(type, course, field, offering, null);
		this.newValue = newValue;
	}

	@Override
	protected Object getField(OffRec rec) {
		if (rec == null) {
			return newValue;
		}
		// TODO Do we need this here at all?
		if ("Duration".equals(field))
			return rec.getDuration();
		return super.getField(rec);
	}

	@Override
	public Object getTarget() {
		if (offering != null)
			return offering.getCourse().getName() + " [" + offering.getId()
					+ "]";
		return course.getName(); // TODO + " [" + record.getCourseId() + "]";
	}

	@SuppressWarnings("unchecked")
	private Object getNewStart() {
		// TODO Self-paced.
		return ((HashMap<String, Object>) newValue).get("start");
	}

	@Override
	public Object getNewValue() {
		if (type == ADD)
			return getNewStart();
		return super.getNewValue();
	}

	@Override
	public void apply(CourseModel model) {
		if (type == ADD) {
			@SuppressWarnings("unchecked")
			HashMap<String, Object> map = (HashMap<String, Object>) newValue;
			course.addOffering(makeOffering(map));
		} else if (type == MODIFY) {
			if ("Active".equals(field)) {
				offering.setActive((Boolean) newValue);
			} else if ("Start".equals(field)) {
				String date = (String) newValue;
				offering.setStart(HttpHelper.parseDate(date));
				offering.setStartStr(date);
			} else if ("Duration".equals(field)) {
				offering.setDuration((Integer) newValue);
			} else if ("Link".equals(field)) {
				offering.setLink((String) newValue);
			} else
				throw new UnsupportedOperationException("Unsupported field "
						+ field);
		} else
			super.apply(model);
	}

	public static OffRec makeOffering(HashMap<String, Object> map) {
		int id = -(Integer) map.get("guid");
		String startStr = (String) map.get("start");
		Date start = HttpHelper.parseDate(startStr);
		int duration = 1; // TODO
		int spread = 1; // TODO
		String home = EdxModelAdapter.getCleanUrl(map);
		boolean active = EdxModelAdapter.getActiveStatus(map);
		return new OffRec(id, start, duration, spread, home, active, startStr);
	}
}
