package classviewer.changes;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;
import classviewer.model.Source;

public class Coursera2OfferingChange extends Change {

	private CourseRec course;
	private HashMap<String, Object> newMap;
	private OffRec oldRecord;
	private String field;

	public static Coursera2OfferingChange makeAdd(CourseRec rec,
			HashMap<String, Object> map) {
		Coursera2OfferingChange me = new Coursera2OfferingChange(Change.ADD);
		me.course = rec;
		me.newMap = map;
		me.setOrder();
		return me;
	}

	public static Coursera2OfferingChange makeChange(
			HashMap<String, Object> session, OffRec oldRecord, String field) {
		Coursera2OfferingChange me = new Coursera2OfferingChange(Change.MODIFY);
		me.course = oldRecord.getCourse();
		me.newMap = session;
		me.oldRecord = oldRecord;
		me.field = field;
		me.setOrder();
		return me;
	}

	private void setOrder() {
		if (type == ADD)
			order = 3;
		else if (type == DELETE)
			order = 5;
		else
			order = 4;
	}

	private Coursera2OfferingChange(String type) {
		super(Source.COURSERA, type);
	}

	@Override
	public String getDescription() {
		if (field != null && !field.isEmpty())
			return field;
		return "Offering";
	}

	@Override
	public Object getTarget() {
		if (course == null)
			return "??";
		String target = "[" + (course.isSelfStudy() ? "#" : "")
				+ course.getStatus() + "]" + course.getName();
		if (oldRecord != null) {
			target += "[" + oldRecord.getId() + "]";
		} else if (newMap != null) {
			target += "[" + newMap.get("id") + "]";
		}
		return target;
	}

	public static Date getStartDate(HashMap<String, Object> map) {
		Long startDay = (Long) map.get("startDay");
		Long startMon = (Long) map.get("startMonth");
		Long startYear = (Long) map.get("startYear");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (startDay == null)
			startDay = 1L;
		cal.set(Calendar.DAY_OF_MONTH, startDay.intValue());
		if (startMon == null)
			startMon = 1L;
		cal.set(Calendar.MONTH, startMon.intValue() - 1);
		if (startYear != null) {
			// No year => no date
			cal.set(Calendar.YEAR, startYear.intValue());
			return cal.getTime();
		}
		return null;
	}

	public static String safeString(Date date) {
		if (date == null)
			return null;
		return OffRec.dformat.format(date);
	}

	public static int getDurationWeeks(HashMap<String, Object> map) {
		String str = (String) map.get("durationString");
		if (str == null)
			return 1;
		// Assume the string is "N weeks".
		try {
			return Integer.parseInt(str.substring(0, str.indexOf(" ")));
		} catch (Exception e) {
			System.out.println("Cannot parse durationString [" + str + "]");
			return 1;
		}
	}

	@Override
	public Object getNewValue() {
		if (type == ADD)
			return safeString(getStartDate(newMap));
		if ("Active".equals(field))
			return newMap.get("active");
		if ("Start".equals(field))
			return safeString(getStartDate(newMap));
		if ("Duration".equals(field))
			return getDurationWeeks(newMap);
		return "???";
	}

	@Override
	public Object getOldValue() {
		if (type == ADD)
			return "";
		if ("Active".equals(field))
			return oldRecord.isActive();
		if ("Start".equals(field))
			return safeString(oldRecord.getStart());
		if ("Duration".equals(field))
			return oldRecord.getDuration();
		return "???";
	}

	@Override
	public void apply(CourseModel model) {
		// TODO Auto-generated method stub
		System.out.println("Add offering");
	}
}
