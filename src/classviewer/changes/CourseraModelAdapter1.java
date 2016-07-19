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

public class CourseraModelAdapter1 implements CourseraModelAdapter {

	private static final long MSEC_IN_WEEK = 24 * 7 * 3600 * 1000;

	private HashMap<String, Object> json;
	private HashMap<String, HashMap<String, Object>> universities = new HashMap<String, HashMap<String, Object>>();
	private HashMap<String, String> uniIdsByShortName = new HashMap<String, String>();
	private HashMap<String, HashMap<String, Object>> categories = new HashMap<String, HashMap<String, Object>>();
	private HashMap<String, HashMap<String, Object>> courses = new HashMap<String, HashMap<String, Object>>();

	@SuppressWarnings("unchecked")
	public void load(String courseraUrl) throws IOException {
		URL url = new URL(courseraUrl);
		InputStream stream = url.openStream();
		InputStreamReader reader = new InputStreamReader(stream);
		this.json = (HashMap<String, Object>) JsonParser.parse(reader);
		stream.close();

		// Extract all universities and categories.
		universities.clear();
		uniIdsByShortName.clear();
		HashMap<String, Object> map = (HashMap<String, Object>) json
				.get("linked");
		ArrayList<Object> list = (ArrayList<Object>) map.get("partners.v1");
		for (Object o : list) {
			map = (HashMap<String, Object>) o;
			String id = String.valueOf(map.get("id"));
			universities.put(id, map);
			uniIdsByShortName.put((String) map.get("shortName"), id);
		}
		categories.clear();
		map = (HashMap<String, Object>) json.get("paging");
		map = (HashMap<String, Object>) map.get("facets");
		map = (HashMap<String, Object>) map.get("categories");
		list = (ArrayList<Object>) map.get("facetEntries");
		for (Object o : list) {
			map = (HashMap<String, Object>) o;
			categories.put((String) map.get("id"), map);
		}

		// Extract courses.
		list = (ArrayList<Object>) json.get("elements");
		courses.clear();
		for (Object o : list) {
			map = (HashMap<String, Object>) o;
			courses.put(String.valueOf(map.get("id")), map);
		}

		// Attach sessions to the courses. 
		map = (HashMap<String, Object>) json.get("linked");
		list = (ArrayList<Object>) map.get("onDemandSessions.v1");
		for (Object o : list) {
			map = (HashMap<String, Object>) o;
			HashMap<String, Object> course = courses.get(map.get("courseId"));
			if (course == null) {
				System.err.println("Have session without a class: " + map);
				continue;
			}
			ArrayList<Object> sessions = (ArrayList<Object>) course
					.get("sessions");
			if (sessions == null) {
				sessions = new ArrayList<Object>();
				course.put("sessions", sessions);
			}
			sessions.add(o);
		}
	}

	public ArrayList<Change> collectChanges(CourseModel model) {
		ArrayList<Change> list = new ArrayList<Change>();

		// Changed universities
		HashSet<String> newIds = new HashSet<String>(this.uniIdsByShortName.keySet());
		for (DescRec rec : model.getUniversities(Source.COURSERA)) {
			if (!newIds.contains(rec.getId())) {
				list.add(DescChange.delete(DescChange.UNIVERSITY, rec));
			} else {
				diffDesc(DescChange.UNIVERSITY, list, rec,
						 this.universities.get(this.uniIdsByShortName.get(rec.getId())));
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			HashMap<String, Object> map = this.universities
					.get(this.uniIdsByShortName.get(id));
			list.add(DescChange.add(DescChange.UNIVERSITY, Source.COURSERA, id,
					(String) map.get("name"), (String) map.get("description")));
		}

		// Changed categories
		newIds = new HashSet<String>(this.categories.keySet());
		for (DescRec rec : model.getCategories(Source.COURSERA)) {
			if (!newIds.contains(rec.getId())) {
				list.add(DescChange.delete(DescChange.CATEGORY, rec));
			} else {
				diffDesc(DescChange.CATEGORY, list, rec,
						this.categories.get(rec.getId()));
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			HashMap<String, Object> map = this.categories.get(id);
			list.add(DescChange.add(DescChange.CATEGORY, Source.COURSERA, id,
					(String) map.get("name"), (String) map.get("description")));
		}

		// Changed classes and offerings
		HashSet<String> newCIds = new HashSet<String>(this.courses.keySet());
		for (CourseRec rec : model.getCourses(Source.COURSERA)) {
			if (!newCIds.contains(rec.getId())) {
				list.add(CourseChange.delete(rec));
			} else {
				diffCourse(list, rec, this.courses.get(rec.getId()), model);
			}
			newCIds.remove(rec.getId());
		}
		for (String id : newCIds) {
			HashMap<String, Object> map = this.courses.get(id);
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, Object>> lst = (ArrayList<HashMap<String, Object>>) map
					.get("sessions");
			boolean selfStudy = lst == null || lst.isEmpty();
			CourseRec record = new CourseRec(Source.COURSERA, id,
					(String) map.get("slug"), (String) map.get("name"),
					(String) map.get("description"), null, null, null, selfStudy);
			if (lst != null) {
				for (HashMap<String, Object> mp : lst) {
					OffRec off = makeOffering(mp);
					if (newEnough(off))
						record.addOffering(off);
				}
			}
			@SuppressWarnings("unchecked")
			ArrayList<String> categories = (ArrayList<String>) map
					.get("categories");
			ArrayList<String> universities = getIncomingUniversities(map);
			list.add(CourseChange.add(record, categories, universities));
		}

		return list;
	}

	private void diffDesc(int what, ArrayList<Change> list, DescRec rec,
			HashMap<String, Object> map) {
		// Name or description changed?
		String b = (String) map.get("name");
		if (Change.fieldChanged(rec.getName(), b)) {
			list.add(DescChange.setName(rec, b));
		}
		b = (String) map.get("description");
		if (Change.fieldChanged(rec.getDescription(), b)) {
			list.add(DescChange.setDescription(rec, b));
		}
	}

	@SuppressWarnings("unchecked")
	private ArrayList<String> getIncomingUniversities(
			HashMap<String, Object> map) {
		ArrayList<String> incoming = new ArrayList<String>();
		for (Object key : (ArrayList<Object>) map.get("partnerIds")) {
			HashMap<String, Object> uni = universities.get(key);
			if (uni == null) {
				System.err.println("Class refers to unknown university: " + map);
				continue;
			}
			incoming.add((String) uni.get("shortName"));
		}
		return incoming;
	}

	@SuppressWarnings("unchecked")
	private void diffCourse(ArrayList<Change> list, CourseRec rec,
			HashMap<String, Object> map, CourseModel model) {
		// Individual fields
		String nv = (String) map.get("slug");
		if (Change.fieldChanged(rec.getShortName(), nv))
			list.add(CourseChange.setShortName(rec, nv));
		nv = (String) map.get("name");
		if (Change.fieldChanged(rec.getName(), nv))
			list.add(CourseChange.setName(rec, nv));
		nv = (String) map.get("description");
		if (Change.fieldChanged(rec.getDescription(), nv))
			list.add(CourseChange.setDescription(rec, nv));

		// Categories
		ArrayList<String> incoming = (ArrayList<String>) map.get("categories");
		HashSet<String> existing = CourseRec.idSet(rec.getCategories());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(CourseChange.setCategories(rec, incoming));
		}

		// Universities
		incoming = getIncomingUniversities(map);
		existing = CourseRec.idSet(rec.getUniversities());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(CourseChange.setUniversities(rec, incoming));
		}

		// Offerings
		ArrayList<HashMap<String, Object>> lst = (ArrayList<HashMap<String, Object>>) map
				.get("sessions");
		if (lst == null)
			lst = new ArrayList<HashMap<String, Object>>();
		HashSet<String> newIds = new HashSet<String>();
		for (HashMap<String, Object> o : lst)
			newIds.add((String) o.get("id"));
		for (OffRec or : rec.getOfferings()) {
			if (!newIds.contains(or.getId())) {
				list.add(OfferingChange.delete(or));
			} else {
				HashMap<String, Object> mp = null;
				for (HashMap<String, Object> o : lst) {
					mp = o;
					if (mp.get("id").equals(or.getId()))
						break;
				}
				diffOffering(list, or, mp);
			}
			newIds.remove(or.getId());
		}
		for (String id : newIds) {
			HashMap<String, Object> mp = null;
			for (Object o : lst) {
				mp = (HashMap<String, Object>) o;
				if (mp.get("id").equals(id)) {
					OffRec off = makeOffering(mp);
					if (newEnough(off))
						list.add(OfferingChange.add(rec, off));
					break;
				}
			}
		}
	}

	private boolean newEnough(OffRec off) {
		Date date = off.getStart();
		if (date == null)
			return true;
		date = new Date(date.getTime() + off.getDuration() * MSEC_IN_WEEK);
		return date.after(new Date()); // Class ends after today.
	}

	@SuppressWarnings("deprecation")
	private OffRec makeOffering(HashMap<String, Object> map) {
		Long start = (Long) map.get("startedAt");
		start = (start / 1000) * 1000; // Get rid of milliseconds.
		Long end = (Long) map.get("endedAt");
		Date startDate = new Date(start);
		// Cut to day start.
		startDate.setHours(0);
		startDate.setMinutes(0);
		startDate.setSeconds(0);
		int dur = 1;
		if (end != null) {
			dur = (int) Math.ceil((end - start) / (double) MSEC_IN_WEEK);
		}
		return new OffRec((String) map.get("id"), startDate, dur, null, false,
				null);
	}

	private void diffOffering(ArrayList<Change> list, OffRec rec,
			HashMap<String, Object> map) {
		OffRec inc = makeOffering(map);
		if (inc.getStart() != null) {
			if (!inc.getStart().equals(rec.getStart()))
				list.add(OfferingChange.setStart(rec, inc.getStart()));
		}
		if (inc.getDuration() != rec.getDuration())
			list.add(OfferingChange.setDuration(rec, inc.getDuration()));
	}
}
