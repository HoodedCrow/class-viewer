package classviewer.changes;

import java.util.ArrayList;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public class EdxCourseChange extends CourseChange {

	private ArrayList<EdxRecord> list;

	public EdxCourseChange(String type, String field, CourseRec course,
			ArrayList<EdxRecord> list, CourseModel model) {
		// Passing NULL for JSON
		super(type, field, course, null, model);
		this.list = list;
		if (list != null)
			created = makeEdxCourse(list.get(0), model);
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
			setCategories(created, model);
			setUniverisities(created, model);
			created.updateId(model.getNewNegativeId());
			// Add offerings
			created.getOfferings().clear();
			for (EdxRecord r : list) {
				OffRec off = EdxOfferingChange.makeEdxOffering(r);
				off.updateId(model.getNewNegativeId());
				created.addOffering(off);
			}
			model.addCourse(created);
		} else 
			super.apply(model);
	}

	@Override
	protected void setCategories(CourseRec rec, CourseModel model) {
		rec.getCategories().clear();
		// None for EdX for now
	}

	@Override
	protected void setUniverisities(CourseRec rec, CourseModel model) {
		rec.getUniversities().clear();
		rec.addUniversity(model.getUniversity(list.get(0).getUniversity()));
	}

	private CourseRec makeEdxCourse(EdxRecord record, CourseModel model) {
		Integer id = 0; // to be overwritten during actual addition
		String shortName = record.getCourseId();
		String name = record.getName();
		String dsc = record.getDescription();
		String instructor = null;
		String language = "en"; // until further notice
		String link = null;
		CourseRec res = new CourseRec(id, shortName, name, dsc, instructor,
				link, language);
		return res;
	}

}
