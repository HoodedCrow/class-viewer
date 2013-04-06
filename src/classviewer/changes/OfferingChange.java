package classviewer.changes;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public class OfferingChange extends Change {

	private String field;
	private CourseRec course;
	private OffRec offering;
	private HashMap<String, Object> json;
	private OffRec created = null;

	public OfferingChange(String type, CourseRec course, String field,
			OffRec offering, HashMap<String, Object> json) {
		super(type);
		this.course = course;
		this.field = field;
		this.offering = offering;
		this.json = json;
		if (json != null)
			created = makeOffering();
	}

	@Override
	public String getDescription() {
		if (type == ADD || type == DELETE)
			return "Offering";
		return field;
	}

	public Object getTarget() {
		if (offering != null)
			return offering.getCourse().getName() + " [" + offering.getId()
					+ "]";
		return course.getName() + " [" + json.get("id") + "]";
	}

	@Override
	public Object getNewValue() {
		if (type == ADD)
			return created.getStartStr();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getOldValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void apply(CourseModel model) {
		if (type == ADD) {
			course.addOffering(created);
		} else if (type == DELETE) {
			Integer id = (Integer) json.get("id");
			course.removeOffering(id);
		} else {
			System.out.println(this);

			// TODO Auto-generated method stub
		}
	}

	private OffRec makeOffering() {
		Integer id = (Integer) json.get("id");

		// Some of these might be missing
		Integer startDay = (Integer) json.get("start_day");
		Integer startMon = (Integer) json.get("start_month");
		Integer startYear = (Integer) json.get("start_year");
		int spread = 1;
		Date start = null;
		String startStr = (String) json.get("start_date_string");

		Calendar cal = Calendar.getInstance();
		if (startDay == null)
			startDay = 1;
		cal.set(Calendar.DAY_OF_MONTH, startDay);
		if (startMon == null) {
			// TODO: pull season?
			spread = 4;
			startMon = 1;
		}
		cal.set(Calendar.MONTH, startMon - 1);
		if (startYear != null) {
			// No year => no date
			cal.set(Calendar.YEAR, startYear);
			start = cal.getTime();
		}
		if (startStr == null && start != null)
			startStr = OffRec.dformat.format(start);

		String durStr = (String) json.get("duration_string");
		int duration = 1;
		if (durStr != null)
			try {
				String s = durStr.trim();
				duration = Integer.parseInt(s.substring(0, s.indexOf(" ")));
			} catch (NumberFormatException e) {
				System.err.println("Cannot parse duration " + durStr);
			}

		Boolean active = (Boolean) json.get("active");
		String home = (String) json.get("home_link");
		return new OffRec(id, start, duration, spread, home, active, startStr,
				durStr);
	}
}
