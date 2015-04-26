package classviewer.changes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;
import classviewer.model.OffRec;
import classviewer.model.Source;

public class CourseraModelAdapter2 implements CourseraModelAdapter {
	// List of maps.
	private ArrayList<Object> courses;
	// Inner HashMap {id, lastName, firstName}
	private HashMap<Object, HashMap<String, Object>> instructors;
	// Inner HashMap { id, name, homeLink, shortName }
	private HashMap<Object, HashMap<String, Object>> universities;
	// Inner HashMap { id, durationWeeks, homeLink, courseId }
	private HashMap<Object, HashMap<String, Object>> sessions;
	// Inner HashMap { id, name }
	private HashMap<Object, HashMap<String, Object>> categories;

	@SuppressWarnings("unchecked")
	public void load(String courseraUrl) throws IOException {
		URL url = new URL(courseraUrl);
		InputStream stream = url.openStream();
		InputStreamReader reader = new InputStreamReader(stream);
		HashMap<String, Object> json = (HashMap<String, Object>) JsonParser
				.parse(reader);
		// Ignore empty "paging"
		this.courses = (ArrayList<Object>) json.get("elements");
		HashMap<String, Object> linked = (HashMap<String, Object>) json
				.get("linked");
		instructors = parseById((ArrayList<Object>) linked
				.get("instructors.v1"));
		sessions = parseById((ArrayList<Object>) linked.get("v1Sessions.v1"));
		universities = parseById((ArrayList<Object>) linked.get("partners.v1"));
		categories = parseById((ArrayList<Object>) linked.get("categories.v1"));
		stream.close();
	}

	private HashMap<Object, HashMap<String, Object>> parseById(
			ArrayList<Object> list) {
		HashMap<Object, HashMap<String, Object>> map = new HashMap<Object, HashMap<String, Object>>();
		for (Object o : list) {
			@SuppressWarnings("unchecked")
			HashMap<String, Object> inner = (HashMap<String, Object>) o;
			map.put(inner.get("id"), inner);
		}
		return map;
	}

	public ArrayList<Change> collectChanges(CourseModel model) {
		ArrayList<Change> list = new ArrayList<Change>();
		collectCategoryChanges(model, list);
		collectUniversityChanges(model, list);
		collectCourseChanges(model, list);
		return list;
	}

	@SuppressWarnings("unchecked")
	private void collectCourseChanges(CourseModel model, ArrayList<Change> list) {
		// Copy list and prune
		ArrayList<HashMap<String, Object>> newCourses = new ArrayList<HashMap<String, Object>>();
		for (Object c : courses)
			newCourses.add((HashMap<String, Object>) c);

		for (CourseRec rec : model.getCourses(Source.COURSERA)) {
			// Find new course
			HashMap<String, Object> found = null;
			for (HashMap<String, Object> c : newCourses) {
				if (rec.getShortName().equals(c.get("slug"))) {
					found = c; // Check id as well?
					break;
				}
			}
			if (found == null) {
				if (rec.getId() > 0)
					list.add(new CourseChange(Source.COURSERA, Change.DELETE,
							null, rec, null, model));
			} else {
				diffCourse(list, rec, found, model);
				newCourses.remove(found);
			}
		}
		for (HashMap<String, Object> c : newCourses) {
			list.add(Coursera2CourseChange.makeAdd(c,
					extractSessions(c.get("id"))));
		}
	}

	/*
	 * {"id":"v1-2","slug":"ml","courseType":"v1.session","name":"Machine Learning"
	 * ,"primaryLanguages":["en"],"instructorIds":["1244"],"partnerIds":["1"],
	 * "categories":["stats","cs-ai"],"description":"Learn about ... ",
	 * "workload":"5-7 hours/week","display":true}
	 */
	@SuppressWarnings("unchecked")
	private void diffCourse(ArrayList<Change> list, CourseRec rec,
			HashMap<String, Object> found, CourseModel model) {
		// Name, description
		String newString = (String) found.get("name");
		if (checkStringChange(rec.getName(), newString))
			list.add(Coursera2CourseChange.makeChange(found, rec, "Name"));
		newString = (String) found.get("description");
		if (checkStringChange(rec.getDescription(), newString))
			list.add(Coursera2CourseChange
					.makeChange(found, rec, "Description"));

		// Language(s)
		ArrayList<Object> langs = (ArrayList<Object>) found
				.get("primaryLanguages");
		if (langs == null || langs.isEmpty()) {
			newString = null;
		} else {
			newString = (String) langs.get(0);
			if (langs.size() > 1)
				System.out.println("Multiple languages for course ["
						+ found.get("name") + "]: " + langs);
		}
		if (checkStringChange(rec.getLanguage(), newString))
			list.add(Coursera2CourseChange.makeChange(found, rec, "Language"));

		if (Coursera2CourseChange.getNewScheduled(found) != rec.isSelfStudy())
			System.out.println("Course self-study flag flipped (ignoring): "
					+ found);

		ArrayList<Object> incoming = (ArrayList<Object>) found
				.get("categories");
		HashSet<String> existing = CourseRec.idSet(rec.getCategories());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(Coursera2CourseChange.makeChange(found, rec, "Categories",
					existing, incoming));
		}

		incoming = Coursera2CourseChange.getUniversityShortNames(found,
				universities);
		existing = CourseRec.idSet(rec.getUniversities());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(Coursera2CourseChange.makeChange(found, rec,
					"Universities", existing, incoming));
		}

		String newInstructorString = Coursera2CourseChange.getInstructorString(
				found, instructors);
		if (!newInstructorString.equals(rec.getInstructor()))
			list.add(Coursera2CourseChange.makeChange(found, rec, "Instructor",
					rec.getInstructor(), newInstructorString));

		// Sessions.
		ArrayList<HashMap<String, Object>> ses = extractSessions(found
				.get("id"));
		for (OffRec or : rec.getOfferings()) {
			HashMap<String, Object> session = null;
			for (HashMap<String, Object> map : ses) {
				Long id = (Long) map.get("id");
				if (id == or.getId()) {
					session = map;
					break;
				}
			}
			if (session == null) {
				// Do not delete sessions for scheduled courses. This version of
				// JSON does not include all of them.
				if (rec.isSelfStudy())
					list.add(new OfferingChange(Source.COURSERA, Change.DELETE,
							rec, null, or, null));
			} else {
				diffOffering(list, or, session);
				ses.remove(session);
			}
		}
		for (HashMap<String, Object> map : ses) {
			list.add(Coursera2OfferingChange.makeAdd(rec, map));
		}
	}

	/*
	 * {"id":973221,"courseId":"v1-1961","homeLink":
	 * "https://class.coursera.org/ntuem-001/","status":1,
	 * "active":false,"durationString" :"6 weeks",
	 * "startDay":1,"startMonth":9,"startYear":2015,
	 * "instructorIds":["4251483"],"selfStudy":false}
	 */
	private void diffOffering(ArrayList<Change> list, OffRec oldRecord,
			HashMap<String, Object> session) {
		String newString = (String) session.get("homeLink");
		if (checkStringChange(oldRecord.getLink(), newString))
			list.add(Coursera2OfferingChange.makeChange(session, oldRecord,
					"Link"));
		boolean newActive = (Boolean) session.get("active");
		if (newActive != oldRecord.isActive())
			list.add(Coursera2OfferingChange.makeChange(session, oldRecord,
					"Active"));
		int newDuration = Coursera2OfferingChange.getDurationWeeks(session);
		if (newDuration != oldRecord.getDuration())
			list.add(Coursera2OfferingChange.makeChange(session, oldRecord,
					"Duration"));
		Date newDate = Coursera2OfferingChange.getStartDate(session);
		if (newDate == null && oldRecord.getStart() != null || newDate != null
				&& !newDate.equals(oldRecord.getStart()))
			list.add(Coursera2OfferingChange.makeChange(session, oldRecord,
					"Start"));
	}

	private ArrayList<HashMap<String, Object>> extractSessions(Object courseId) {
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
		ArrayList<Object> remove = new ArrayList<Object>();
		for (Object key : this.sessions.keySet()) {
			HashMap<String, Object> ses = this.sessions.get(key);
			if (courseId.equals(ses.get("courseId"))) {
				list.add(ses);
				remove.add(key);
			}
		}
		for (Object key : remove)
			this.sessions.remove(key);
		return list;
	}

	private void collectCategoryChanges(CourseModel model,
			ArrayList<Change> list) {
		HashSet<String> newIds = new HashSet<String>();
		for (Object id : this.categories.keySet()) {
			newIds.add((String) id);
		}
		for (DescRec rec : model.getCategories(Source.COURSERA)) {
			if (!newIds.contains(rec.getId())) {
				list.add(new DescChange(Source.COURSERA, DescChange.CATEGORY,
						Change.DELETE, "Category", rec, null));
			} else {
				HashMap<String, Object> map = this.categories.get(rec.getId());
				String newName = (String) map.get("name");
				if (checkStringChange(rec.getName(), newName)) {
					list.add(new DescChange(Source.COURSERA,
							DescChange.CATEGORY, Change.MODIFY, "Name", rec,
							fakeCategoryMap(map)));
				}
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			list.add(new DescChange(Source.COURSERA, DescChange.CATEGORY,
					Change.ADD, "Category", null,
					fakeCategoryMap(this.categories.get(id))));
		}
	}

	private void collectUniversityChanges(CourseModel model,
			ArrayList<Change> list) {
		HashSet<String> newIds = new HashSet<String>();
		for (HashMap<String, Object> map : this.universities.values()) {
			String name = (String) map.get("shortName");
			if (name != null)
				newIds.add(name);
		}
		for (DescRec rec : model.getUniversities(Source.COURSERA)) {
			if (!newIds.contains(rec.getId())) {
				list.add(new DescChange(Source.COURSERA, DescChange.UNIVERSITY,
						Change.DELETE, "University", rec, null));
			} else {
				HashMap<String, Object> map = findUniversity(rec.getId());
				String newString = (String) map.get("name");
				if (checkStringChange(rec.getName(), newString)) {
					list.add(new DescChange(Source.COURSERA,
							DescChange.UNIVERSITY, Change.MODIFY, "Name", rec,
							fakeUniversityMap(map)));
				}
				newString = (String) map.get("description");
				if (checkStringChange(rec.getDescription(), newString)) {
					list.add(new DescChange(Source.COURSERA,
							DescChange.UNIVERSITY, Change.MODIFY,
							"Description", rec, fakeUniversityMap(map)));
				}
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			list.add(new DescChange(Source.COURSERA, DescChange.UNIVERSITY,
					Change.ADD, "University", null,
					fakeUniversityMap(findUniversity(id))));
		}
	}

	private HashMap<String, Object> fakeUniversityMap(
			HashMap<String, Object> source) {
		source.put("short_name", source.get("shortName"));
		return source;
	}

	private HashMap<String, Object> fakeCategoryMap(
			HashMap<String, Object> source) {
		source.put("short_name", source.get("id"));
		return source;
	}

	private HashMap<String, Object> findUniversity(String shortName) {
		for (HashMap<String, Object> map : this.universities.values()) {
			String name = (String) map.get("shortName");
			if (shortName.equals(name))
				return map;
		}
		return null;
	}

	private boolean checkStringChange(String oldString, String newString) {
		if (oldString != null && oldString.isEmpty())
			oldString = null;
		if (newString != null && newString.isEmpty())
			newString = null;
		return (oldString == null && newString != null || oldString != null
				&& !oldString.equals(newString));
	}
}
