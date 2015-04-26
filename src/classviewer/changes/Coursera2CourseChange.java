package classviewer.changes;

import java.util.ArrayList;
import java.util.HashMap;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.Source;

public class Coursera2CourseChange extends Change {
	private HashMap<String, Object> newRec = null;
	private CourseRec oldRec = null;
	private String field = null;
	private Object oldValue = null;
	private Object newValue = null;
	private ArrayList<HashMap<String, Object>> sessions;

	public static Coursera2CourseChange makeAdd(HashMap<String, Object> newRec,
			ArrayList<HashMap<String, Object>> sessions) {
		Coursera2CourseChange me = new Coursera2CourseChange(Change.ADD);
		me.newRec = newRec;
		me.sessions = sessions;
		me.setOrder();
		return me;
	}

	public static Coursera2CourseChange makeChange(
			HashMap<String, Object> newRec, CourseRec oldRec, String field) {
		Coursera2CourseChange me = new Coursera2CourseChange(Change.MODIFY);
		me.newRec = newRec;
		me.oldRec = oldRec;
		me.field = field;
		me.setOrder();
		return me;
	}

	public static Change makeChange(HashMap<String, Object> newRec,
			CourseRec oldRec, String field, Object oldValue, Object newValue) {
		Coursera2CourseChange me = new Coursera2CourseChange(Change.MODIFY);
		me.newRec = newRec;
		me.oldRec = oldRec;
		me.field = field;
		me.oldValue = oldValue;
		me.newValue = newValue;
		me.setOrder();
		return me;
	}

	private void setOrder() {
		if (type == ADD)
			order = 2;
		else if (type == DELETE)
			order = 6;
		else
			order = 4;
	}

	private Coursera2CourseChange(String type) {
		super(Source.COURSERA, type);
	}

	@Override
	public String getDescription() {
		if (field != null && !field.isEmpty())
			return field;
		return "Course";
	}

	private String getNewName() {
		return (String) newRec.get("name");
	}

	@Override
	public Object getTarget() {
		// Deletions are done via CourseChange, so this is never null.
		return getNewName();
	}

	public static boolean getNewScheduled(HashMap<String, Object> map) {
		Object type = map.get("courseType");
		if ("v1.session".equals(type) || "v1.capstone".equals(type))
			return true;
		if ("v2.ondemand".equals(type))
			return false;
		System.out.println("Cannot figure out if this is scheduled: " + map);
		return true;
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<Object> getUniversityShortNames(
			HashMap<String, Object> map,
			HashMap<Object, HashMap<String, Object>> universities) {
		ArrayList<Object> ids = (ArrayList<Object>) map.get("partnerIds");
		ArrayList<Object> result = new ArrayList<Object>();
		for (Object id : ids) {
			Object uni = universities.get(id);
			if (uni == null) {
				System.out.println("Unkown university id " + id);
				continue;
			}
			uni = ((HashMap<String, Object>) uni).get("shortName");
			if (uni == null) {
				System.out.println("No short name for partner " + id);
			} else {
				result.add(uni);
			}
		}
		return result;
	}

	public static String getInstructorString(HashMap<String, Object> map,
			HashMap<Object, HashMap<String, Object>> instructors) {
		@SuppressWarnings("unchecked")
		ArrayList<Object> ids = (ArrayList<Object>) map.get("instructorIds");
		String result = "";
		for (Object id : ids) {
			HashMap<String, Object> ins = instructors.get(id);
			if (ins == null) {
				System.out.println("Unkown instructor id " + id);
				continue;
			}
			result += ", " + ins.get("firstName") + " " + ins.get("lastName");
		}
		return result.substring(2);
	}

	@Override
	public Object getNewValue() {
		if (this.type == Change.ADD) {
			if (getNewScheduled(newRec))
				return "++ Scheduled";
			return ".. On demand";
		}
		// Deletions are done via CourseChange, so new value is never null.
		if ("Name".equals(field))
			return getNewName();
		if ("Description".equals(field))
			return newRec.get("description");
		if ("Language".equals(field))
			return newRec.get("primaryLanguages");
		if (newValue != null)
			return newValue;
		return "!! " + field;
	}

	@Override
	public Object getOldValue() {
		if (this.type == Change.ADD)
			return "";
		if (oldRec == null)
			return "??";
		if ("Name".equals(field))
			return oldRec.getName();
		if ("Description".equals(field))
			return oldRec.getDescription();
		if ("Language".equals(field))
			return oldRec.getLanguage();
		if (oldValue != null)
			return oldValue;
		return "!! " + field;
	}

	@Override
	public void apply(CourseModel model) {
		// TODO Auto-generated method stub
		System.out.println("TODO");
	}
}
