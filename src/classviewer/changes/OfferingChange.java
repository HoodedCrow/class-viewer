package classviewer.changes;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;
import classviewer.model.Source;

public class OfferingChange extends Change {

	protected String field;
	protected CourseRec course;
	protected OffRec offering;
	private HashMap<String, Object> json;
	protected OffRec created = null;

	public OfferingChange(Source source, String type, CourseRec course,
			String field, OffRec offering, HashMap<String, Object> json) {
		super(source, type);
		this.course = course;
		this.field = field;
		this.offering = offering;
		this.json = json;
		if (json != null)
			created = makeOffering(json);
		if (type == ADD)
			order = 3;
		else if (type == DELETE)
			order = 5;
		else
			order = 4;
	}

	@Override
	public String getDescription() {
		if (type == ADD || type == DELETE)
			return "Offering";
		return field;
	}

	public Object getTarget() {
		CourseRec cr;
		if (offering != null)
			cr = offering.getCourse();
		else
			cr = course;
		String res = "[" + cr.getStatus() + "] " + cr.getName();
		if (offering != null)
			return res + " [" + offering.getId() + "]";
		return res + " [" + json.get("id") + "]";
	}

	@Override
	public Object getNewValue() {
		if (type == ADD)
			return created.getStartStr();
		if (type == DELETE)
			return offering.getStartStr();
		return getField(created);
	}

	protected Object getField(OffRec rec) {
		if ("Spread".equals(field))
			return rec.getSpread();
		if ("Active".equals(field))
			return rec.isActive();
		if ("Start".equals(field))
			return rec.getStartStr();
		if ("Duration".equals(field))
			return rec.getDuration();
		if ("Link".equals(field))
			return rec.getLink();
		return "??? " + field;
	}

	public boolean isActivationChange() {
		return type == MODIFY && "Active".equals(field);
	}

	@Override
	public Object getOldValue() {
		if (type == MODIFY)
			return getField(offering);
		return null;
	}

	@Override
	public void apply(CourseModel model) {
		if (type == ADD) {
			course.addOffering(created);
		} else if (type == DELETE) {
			course.removeOffering(this.offering.getId());
		} else {
			if ("Spread".equals(field)) {
				offering.setSpread(created.getSpread());
			} else if ("Active".equals(field)) {
				offering.setActive(created.isActive());
			} else if ("Start".equals(field)) {
				offering.setStart(created.getStart());
				offering.setStartStr(created.getStartStr());
			} else if ("Duration".equals(field)) {
				offering.setDuration(created.getDuration());
			} else if ("Link".equals(field)) {
				offering.setLink(created.getLink());
			} else
				throw new UnsupportedOperationException("Unsupported field "
						+ field);
		}
	}

	public static OffRec makeOffering(HashMap<String, Object> json) {
		Long id = (Long) json.get("id");

		// Some of these might be missing
		Long startDay = (Long) json.get("start_day");
		Long startMon = (Long) json.get("start_month");
		Long startYear = (Long) json.get("start_year");
		int spread = 1;
		Date start = null;
		String startStr = (String) json.get("start_date_string");

		Calendar cal = Calendar.getInstance();
		if (startDay == null)
			startDay = 1L;
		cal.set(Calendar.DAY_OF_MONTH, startDay.intValue());
		if (startMon == null) {
			// TODO: pull season?
			spread = 4;
			startMon = 1L;
		}
		cal.set(Calendar.MONTH, startMon.intValue() - 1);
		if (startYear != null) {
			// No year => no date
			cal.set(Calendar.YEAR, startYear.intValue());
			start = cal.getTime();
		} else {
			spread = 1; // no data
		}
		if (startStr == null && start != null)
			startStr = OffRec.dformat.format(start);

		String durStr = (String) json.get("duration_string");
		if ("".equals(durStr))
			durStr = null;
		int duration = 1;
		if (durStr != null)
			try {
				String s = durStr.trim();
				if (s.indexOf(" ") > 0)
					s = s.substring(0, s.indexOf(" "));
				if (s.indexOf("-") > 0)
					s = s.substring(0, s.indexOf("-"));
				duration = Integer.parseInt(s);
			} catch (Exception e) {
				System.err.println("Cannot parse duration " + durStr);
			}

		Boolean active = (Boolean) json.get("active");
		String home = (String) json.get("home_link");
		return new OffRec(id, start, duration, spread, home, active, startStr);
	}
}
