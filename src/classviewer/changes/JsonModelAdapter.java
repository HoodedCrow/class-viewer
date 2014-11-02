package classviewer.changes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;
import classviewer.model.OffRec;

public class JsonModelAdapter {

	private ArrayList<Object> json;
	private HashMap<String, HashMap<String, Object>> universities = new HashMap<String, HashMap<String, Object>>();
	private HashMap<String, HashMap<String, Object>> categories = new HashMap<String, HashMap<String, Object>>();
	private HashMap<Integer, HashMap<String, Object>> courses = new HashMap<Integer, HashMap<String, Object>>();

	@SuppressWarnings("unchecked")
	public void load(String courseraUrl) throws IOException {
		URL url = new URL(courseraUrl);
		InputStream stream = url.openStream();
		InputStreamReader reader = new InputStreamReader(stream);
		this.json = JsonParser.parse(reader);
		stream.close();

		// Extract all universities and categories
		universities.clear();
		for (Object o : json) {
			HashMap<String, Object> map = (HashMap<String, Object>) o;
			ArrayList<Object> list = (ArrayList<Object>) map
					.get("universities");
			for (Object u : list) {
				map = (HashMap<String, Object>) u;
				universities.put((String) map.get("short_name"), map);
			}
		}
		categories.clear();
		for (Object o : json) {
			HashMap<String, Object> map = (HashMap<String, Object>) o;
			ArrayList<Object> list = (ArrayList<Object>) map.get("categories");
			for (Object u : list) {
				map = (HashMap<String, Object>) u;
				categories.put((String) map.get("short_name"), map);
			}
		}
		courses.clear();
		for (Object o : json) {
			HashMap<String, Object> map = (HashMap<String, Object>) o;
			courses.put((Integer) map.get("id"), map);
		}
	}

	public ArrayList<Change> collectChanges(CourseModel model) {
		ArrayList<Change> list = new ArrayList<Change>();

		// Changed universities
		HashSet<String> newIds = new HashSet<String>(this.universities.keySet());
		for (DescRec rec : model.getUniversities()) {
			if (!newIds.contains(rec.getId())) {
				String d = rec.getDescription();
				if (!rec.getId().equals("udacity")
						&& (d == null || !d.contains("EdX")))
					list.add(new DescChange(DescChange.UNIVERSITY,
							Change.DELETE, "University", rec, null));
			} else {
				diffDesc(DescChange.UNIVERSITY, list, rec,
						this.universities.get(rec.getId()));
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			list.add(new DescChange(DescChange.UNIVERSITY, Change.ADD,
					"University", null, this.universities.get(id)));
		}

		// Changed categories
		newIds = new HashSet<String>(this.categories.keySet());
		for (DescRec rec : model.getCategories()) {
			if (!newIds.contains(rec.getId())) {
				list.add(new DescChange(DescChange.CATEGORY, Change.DELETE,
						"Category", rec, null));
			} else {
				diffDesc(DescChange.CATEGORY, list, rec,
						this.categories.get(rec.getId()));
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			list.add(new DescChange(DescChange.CATEGORY, Change.ADD,
					"Category", null, this.categories.get(id)));
		}

		// Changed classes and offerings
		HashSet<Integer> newCIds = new HashSet<Integer>(this.courses.keySet());
		for (CourseRec rec : model.getCourses()) {
			if (!newCIds.contains(rec.getId())) {
				if (rec.getId() > 0)
					list.add(new CourseChange(Change.DELETE, null, rec, null,
							model));
			} else {
				diffCourse(list, rec, this.courses.get(rec.getId()), model);
			}
			newCIds.remove(rec.getId());
		}
		for (Integer id : newCIds) {
			list.add(new CourseChange(Change.ADD, null, null, this.courses
					.get(id), model));
		}

		return list;
	}

	private void diffDesc(int what, ArrayList<Change> list, DescRec rec,
			HashMap<String, Object> map) {
		// Name or description changed?
		String a = rec.getName();
		if (a != null && a.isEmpty())
			a = null;
		String b = (String) map.get("name");
		if (b != null && b.isEmpty())
			b = null;
		if (a == null && b != null || a != null && !a.equals(b)) {
			list.add(new DescChange(what, Change.MODIFY, "Name", rec, map));
		}

		a = rec.getDescription();
		if (a != null && a.isEmpty())
			a = null;
		b = (String) map.get("description");
		if (b != null && b.isEmpty())
			b = null;
		if (a == null && b != null || a != null && !a.equals(b)) {
			list.add(new DescChange(what, Change.MODIFY, "Description", rec,
					map));
		}
	}

	@SuppressWarnings("unchecked")
	private void diffCourse(ArrayList<Change> list, CourseRec rec,
			HashMap<String, Object> map, CourseModel model) {
		// Individual fields
		compareCourseField(list, rec.getShortName(),
				(String) map.get("short_name"), "Short name", rec, map, model);
		compareCourseField(list, rec.getName(), (String) map.get("name"),
				"Name", rec, map, model);
		compareCourseField(list, rec.getDescription(),
				(String) map.get("short_description"), "Description", rec, map,
				model);
		compareCourseField(list, rec.getInstructor(),
				(String) map.get("instructor"), "Instructor", rec, map, model);
		compareCourseField(list, rec.getLink(),
				(String) map.get("social_link"), "Link", rec, map, model);
		compareCourseField(list, rec.getLanguage(),
				(String) map.get("language"), "Language", rec, map, model);

		// categories
		ArrayList<Object> incoming = (ArrayList<Object>) map
				.get("category-ids");
		HashSet<String> existing = CourseRec.idSet(rec.getCategories());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(new CourseChange(Change.MODIFY, "Categories", rec, map,
					model));
		}
		// universities
		incoming = (ArrayList<Object>) map.get("university-ids");
		existing = CourseRec.idSet(rec.getUniversities());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(new CourseChange(Change.MODIFY, "Universities", rec, map,
					model));
		}
		// offerings
		ArrayList<Object> lst = (ArrayList<Object>) map.get("courses");
		HashSet<Integer> newIds = new HashSet<Integer>();
		for (Object o : lst)
			newIds.add((Integer) ((HashMap<String, Object>) o).get("id"));
		for (OffRec or : rec.getOfferings()) {
			if (!newIds.contains(or.getId())) {
				if (rec.getId() > 0)
					list.add(new OfferingChange(Change.DELETE, rec, null, or,
							null));
			} else {
				HashMap<String, Object> mp = null;
				for (Object o : lst) {
					mp = (HashMap<String, Object>) o;
					if (mp.get("id").equals(or.getId()))
						break;
				}
				diffOffering(list, or, mp);
			}
			newIds.remove(or.getId());
		}
		for (Integer id : newIds) {
			HashMap<String, Object> mp = null;
			for (Object o : lst) {
				mp = (HashMap<String, Object>) o;
				if (mp.get("id").equals(id))
					break;
			}
			list.add(new OfferingChange(Change.ADD, rec, null, null, mp));
		}
	}

	private void diffOffering(ArrayList<Change> list, OffRec rec,
			HashMap<String, Object> map) {
		// Use as a field parser
		OffRec created = OfferingChange.makeOffering(map);
		if (created.getSpread() != rec.getSpread())
			list.add(new OfferingChange(Change.MODIFY, rec.getCourse(),
					"Spread", rec, map));
		if (created.isActive() != rec.isActive())
			list.add(new OfferingChange(Change.MODIFY, rec.getCourse(),
					"Active", rec, map));
		if (created.getStartStr() == null && rec.getStartStr() != null
				|| created.getStartStr() != null
				&& !created.getStartStr().equals(rec.getStartStr()))
			list.add(new OfferingChange(Change.MODIFY, rec.getCourse(),
					"Start", rec, map));
		if (created.getDuration() != rec.getDuration())
			list.add(new OfferingChange(Change.MODIFY, rec.getCourse(),
					"Duration", rec, map));
		if (created.getLink() == null && rec.getLink() != null
				|| created.getLink() != null
				&& !created.getLink().equals(rec.getLink()))
			list.add(new OfferingChange(Change.MODIFY, rec.getCourse(), "Link",
					rec, map));
	}

	private void compareCourseField(ArrayList<Change> list, String existing,
			String incoming, String tag, CourseRec rec,
			HashMap<String, Object> map, CourseModel model) {
		if (existing != null && existing.isEmpty())
			existing = null;
		if (incoming != null && incoming.isEmpty())
			incoming = null;
		if (existing == null && incoming != null || existing != null
				&& !existing.equals(incoming)) {
			list.add(new CourseChange(Change.MODIFY, tag, rec, map, model));
		}
	}

	public HashSet<String> getCourseLevelKeys() {
		HashSet<String> keys = new HashSet<String>();
		for (Object o : json) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) o;
				keys.addAll(map.keySet());
			} catch (ClassCastException e) {
				// Huh?
			}
		}
		return keys;
	}

	@SuppressWarnings("unchecked")
	public HashSet<String> getOfferingLevelKeys() {
		HashSet<String> keys = new HashSet<String>();
		for (Object o : json) {
			try {
				HashMap<String, Object> map = (HashMap<String, Object>) o;
				ArrayList<Object> list = (ArrayList<Object>) map.get("courses");
				map = (HashMap<String, Object>) list.get(0);
				keys.addAll(map.keySet());
			} catch (ClassCastException e) {
				// Huh?
			} catch (NullPointerException e) {
				System.err.println("Got a high level map in JSON "
						+ "without 'courses' key. "
						+ "Please check if Coursera's format has changed.");
			}
		}
		return keys;
	}
}
