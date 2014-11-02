package classviewer.changes;

import java.util.ArrayList;
import java.util.HashMap;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public class EdxCourseChange extends CourseChange {

	// private ArrayList<EdxRecord> list;
	private Object newValue;

	public EdxCourseChange(String type, String field, CourseRec course,
			Object newValue, CourseModel model) {
		super(type, field, course, null, model);
		this.newValue = newValue;
	}

	public Object valueOf(CourseRec rec) {
		if ("Name".equals(field))
			return rec.getName();
		if ("Description".equals(field))
			return rec.getDescription();
		if ("Short name".equals(field))
			return rec.getShortName();
		if ("Instructor".equals(field))
			return rec.getInstructor();
		if ("Language".equals(field))
			return rec.getLanguage();
		if ("Link".equals(field))
			return rec.getLink();
		// EdX should not change categories or universities, so let these go to
		// the ??? below
		// if ("Categories".equals(field)) return json.get("category-ids");
		// if ("Universities".equals(field)) return json.get("university-ids");
		return "??? " + field;
	}

	@Override
	public void apply(CourseModel model) {
		if (type == ADD) {
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, Object>> list = (ArrayList<HashMap<String, Object>>) newValue;
			CourseRec rec = makeEdxCourse(list.get(0), model);
			for (HashMap<String, Object> map : list) {
				OffRec off = EdxOfferingChange.makeOffering(map);
				rec.addOffering(off);
			}
			model.addCourse(rec);
		} else if (type == MODIFY) {
			// Assume we have a pointer to the record we can change in place.
			assert (course != null);
			if ("Name".equals(field)) {
				course.setName((String) newValue);
			} else if ("Description".equals(field)) {
				course.setDescription((String) newValue);
			} else if ("Short name".equals(field)) {
				course.setShortName((String) newValue);
			} else if ("Instructor".equals(field)) {
				course.setInstructor((String) newValue);
			} else if ("Language".equals(field)) {
				course.setLanguage((String) newValue);
			} else if ("Categories".equals(field)) {
				setCategories(course, model);
			} else if ("Universities".equals(field)) {
				setUniverisities(course, model);
			} else {
				throw new UnsupportedOperationException("Unknown field "
						+ field);
			}
		} else
			super.apply(model);
	}

	@SuppressWarnings("unchecked")
	private String getNewCourseName() {
		Object first = ((ArrayList<Object>) newValue).get(0);
		return (String) ((HashMap<String, Object>) first).get("l");
	}

	public Object getTarget() {
		// For adding new courses, target is the long name.
		if (type == ADD)
			return getNewCourseName();
		return super.getTarget();
	}

	public Object getNewValue() {
		if (type == ADD)
			return getNewCourseName();
		if (type == MODIFY) {
			return newValue;
		}
		return super.getNewValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setCategories(CourseRec rec, CourseModel model) {
		rec.getCategories().clear();
		for (String s : (ArrayList<String>) newValue)
			rec.addCategory(model.getCategory(s));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setUniverisities(CourseRec rec, CourseModel model) {
		rec.getUniversities().clear();
		for (String s : (ArrayList<String>) newValue)
			rec.addUniversity(model.getUniversity(s));
	}

	@SuppressWarnings("unchecked")
	public static CourseRec makeEdxCourse(HashMap<String, Object> map,
			CourseModel model) {
		Integer id = model.getNewNegativeId();
		String short_name = EdxModelAdapter.getCleanCode(map);
		String name = (String) map.get("l");
		String dsc = null; // TODO
		String instructor = null;
		String language = "en"; // until further notice
		String link = null;
		CourseRec res = new CourseRec(id, short_name, name, dsc, instructor,
				link, language);
		for (String s : (ArrayList<String>) map.get("subjects"))
			res.addCategory(model.getCategory(EdxModelAdapter.makeCategoryId(s)));
		for (String s : (ArrayList<String>) map.get("schools"))
			res.addUniversity(model.getUniversity(EdxModelAdapter.makeIdSafe(s)));
		return res;
	}

}
