package classviewer.changes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
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
				list.add(CourseChange.delete(rec));
			} else {
				diffCourse(list, rec, found, model);
				newCourses.remove(found);
			}
		}
		for (HashMap<String, Object> c : newCourses) {
			ArrayList<HashMap<String, Object>> map = extractSessions(c
					.get("id"));
			boolean selfStudy = true;
			for (HashMap<String, Object> one : map) {
				selfStudy &= !getNewScheduled(one);
			}
			String id = String.valueOf(c.get("id"));
			if (id.startsWith("v1-"))
				id = id.substring(3);
			String shortName = (String) c.get("slug");
			String name = (String) c.get("name");
			String description = (String) c.get("description");
			String instructor = getInstructorString(c);
			String link = (String) c.get("homeLink");
			String language = getLanguage(c);
			CourseRec record = new CourseRec(Source.COURSERA, id, shortName,
					name, description, instructor, link, language, selfStudy);
			for (HashMap<String, Object> one : map) {
				record.addOffering(makeOffering(one));
			}
			ArrayList<String> categories = (ArrayList<String>) c
					.get("categories");
			ArrayList<String> universities = getUniversityShortNames(c);
			list.add(CourseChange.add(record, categories, universities));
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
		if (Change.fieldChanged(rec.getName(), newString))
			list.add(CourseChange.setName(rec, newString));
		newString = (String) found.get("description");
		if (Change.fieldChanged(rec.getDescription(), newString))
			list.add(CourseChange.setDescription(rec, newString));

		// Language(s)
		newString = getLanguage(found);
		if (Change.fieldChanged(rec.getLanguage(), newString))
			list.add(CourseChange.setLanguage(rec, newString));

		if (getNewScheduled(found) == rec.isSelfStudy())
			System.out.println("Course self-study flag flipped (ignoring): "
					+ found);

		ArrayList<String> incoming = (ArrayList<String>) found
				.get("categories");
		HashSet<String> existing = CourseRec.idSet(rec.getCategories());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(CourseChange.setCategories(rec, incoming));
		}

		incoming = getUniversityShortNames(found);
		existing = CourseRec.idSet(rec.getUniversities());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(CourseChange.setUniversities(rec, incoming));
		}

		newString = getInstructorString(found);
		if (Change.fieldChanged(rec.getInstructor(), newString))
			list.add(CourseChange.setInstructor(rec, newString));

		// Sessions.
		ArrayList<HashMap<String, Object>> ses = extractSessions(found
				.get("id"));
		for (OffRec or : rec.getOfferings()) {
			HashMap<String, Object> session = null;
			for (HashMap<String, Object> map : ses) {
				String id = (String) map.get("id");
				if (id.equals(or.getId())) {
					session = map;
					break;
				}
			}
			if (session == null) {
				// Do not delete sessions for scheduled courses. This version of
				// JSON does not include all of them.
				if (rec.isSelfStudy())
					list.add(OfferingChange.delete(or));
			} else {
				diffOffering(list, or, session);
				ses.remove(session);
			}
		}
		for (HashMap<String, Object> map : ses) {
			OffRec or = makeOffering(map);
			or.setCourse(rec);
			list.add(OfferingChange.add(rec, or));
		}
	}

	private OffRec makeOffering(HashMap<String, Object> map) {
		Date start = getStartDate(map);
		String id = (String) map.get("id");
		return new OffRec(id, start, getDurationWeeks(map), 
				(String) map.get("homeLink"), (Boolean) map.get("active"), null);
	}

	@SuppressWarnings("unchecked")
	private ArrayList<String> getUniversityShortNames(
			HashMap<String, Object> map) {
		ArrayList<Object> ids = (ArrayList<Object>) map.get("partnerIds");
		ArrayList<String> result = new ArrayList<String>();
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
				result.add((String) uni);
			}
		}
		return result;
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
		if (Change.fieldChanged(oldRecord.getLink(), newString))
			list.add(OfferingChange.setLink(oldRecord, newString));
		boolean newActive = (Boolean) session.get("active");
		if (newActive != oldRecord.isActive())
			list.add(OfferingChange.setActive(oldRecord, newActive));
		long newDuration = getDurationWeeks(session);
		if (newDuration != oldRecord.getDuration())
			list.add(OfferingChange.setDuration(oldRecord, newDuration));
		Date newDate = getStartDate(session);
		if (newDate == null && oldRecord.getStart() != null || newDate != null
				&& !newDate.equals(oldRecord.getStart()))
			list.add(OfferingChange.setStart(oldRecord, newDate));
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
				list.add(DescChange.delete(DescChange.CATEGORY, rec));
			} else {
				HashMap<String, Object> map = this.categories.get(rec.getId());
				String newName = (String) map.get("name");
				if (Change.fieldChanged(rec.getName(), newName)) {
					list.add(DescChange.setName(rec, newName));
				}
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			HashMap<String, Object> map = this.categories.get(id);
			list.add(DescChange.add(DescChange.CATEGORY, Source.COURSERA, id,
					(String) map.get("name"), (String) map.get("description")));
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
				list.add(DescChange.delete(DescChange.UNIVERSITY, rec));
			} else {
				HashMap<String, Object> map = findUniversity(rec.getId());
				String newString = (String) map.get("name");
				if (Change.fieldChanged(rec.getName(), newString)) {
					list.add(DescChange.setName(rec, newString));
				}
				newString = (String) map.get("description");
				if (Change.fieldChanged(rec.getDescription(), newString)) {
					list.add(DescChange.setDescription(rec, newString));
				}
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			HashMap<String, Object> map = null;
			String shortName = null;
			for (HashMap<String, Object> m : this.universities.values()) {
				shortName = (String) m.get("shortName");
				if (id.equals(shortName)) {
					map = m;
					break;
				}
			}
			if (map == null) {
				System.err.println("Cannot find uni by short name " + id
						+ ". This should not happen.");
			} else {
				list.add(DescChange.add(DescChange.UNIVERSITY, Source.COURSERA,
						shortName, (String) map.get("name"),
						(String) map.get("description")));
			}
		}
	}

	private HashMap<String, Object> findUniversity(String shortName) {
		for (HashMap<String, Object> map : this.universities.values()) {
			String name = (String) map.get("shortName");
			if (shortName.equals(name))
				return map;
		}
		return null;
	}

	private String getInstructorString(HashMap<String, Object> map) {
		@SuppressWarnings("unchecked")
		ArrayList<Object> ids = (ArrayList<Object>) map.get("instructorIds");
		if (ids.isEmpty())
			return null;
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

	private static boolean getNewScheduled(HashMap<String, Object> map) {
		Object self = map.get("selfStudy");
		if (self != null)
			return !(Boolean) self;
		Object type = map.get("courseType");
		if ("v1.session".equals(type) || "v1.capstone".equals(type))
			return true;
		if ("v2.ondemand".equals(type))
			return false;
		System.out.println("Cannot figure out if this is scheduled: " + map);
		return true;
	}

	private static Date getStartDate(HashMap<String, Object> map) {
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

	private static int getDurationWeeks(HashMap<String, Object> map) {
		String str = (String) map.get("durationString");
		if (str == null)
			return 1;
		// Assume the string is "N weeks".
		try {
			return Integer.parseInt(str.substring(0, str.indexOf(" ")));
		} catch (Exception e) {
			System.out.println("Cannot parse durationString [" + str + "] in "
					+ map);
			return 1;
		}
	}

	private String getLanguage(HashMap<String, Object> map) {
		@SuppressWarnings("unchecked")
		ArrayList<Object> langs = (ArrayList<Object>) map
				.get("primaryLanguages");
		if (langs == null || langs.isEmpty()) {
			return null;
		} else {
			if (langs.size() > 1)
				System.out.println("Multiple languages for course ["
						+ map.get("name") + "]: " + langs);
			return (String) langs.get(0);
		}
	}
}
