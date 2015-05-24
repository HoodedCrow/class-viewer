package classviewer.changes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;
import classviewer.model.OffRec;
import classviewer.model.Source;

public class CourseraModelAdapter1 implements CourseraModelAdapter {

	private ArrayList<Object> json;
	private HashMap<String, HashMap<String, Object>> universities = new HashMap<String, HashMap<String, Object>>();
	private HashMap<String, HashMap<String, Object>> categories = new HashMap<String, HashMap<String, Object>>();
	private HashMap<String, HashMap<String, Object>> courses = new HashMap<String, HashMap<String, Object>>();

	@SuppressWarnings("unchecked")
	public void load(String courseraUrl) throws IOException {
		URL url = new URL(courseraUrl);
		InputStream stream = url.openStream();
		InputStreamReader reader = new InputStreamReader(stream);
		this.json = (ArrayList<Object>) JsonParser.parse(reader);
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
			courses.put(String.valueOf(map.get("id")), map);
		}
	}

	public ArrayList<Change> collectChanges(CourseModel model) {
		ArrayList<Change> list = new ArrayList<Change>();

		// Changed universities
		HashSet<String> newIds = new HashSet<String>(this.universities.keySet());
		for (DescRec rec : model.getUniversities(Source.COURSERA)) {
			if (!newIds.contains(rec.getId())) {
				list.add(DescChange.delete(DescChange.UNIVERSITY, rec));
			} else {
				diffDesc(DescChange.UNIVERSITY, list, rec,
						this.universities.get(rec.getId()));
			}
			newIds.remove(rec.getId());
		}
		for (String id : newIds) {
			HashMap<String, Object> map = this.universities.get(id);
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
			// selfStudy == false in this parser.
			CourseRec record = new CourseRec(Source.COURSERA, id,
					(String) map.get("short_name"), (String) map.get("name"),
					(String) map.get("description"),
					(String) map.get("instructor"),
					(String) map.get("social_link"),
					(String) map.get("language"), false);
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, Object>> lst = (ArrayList<HashMap<String, Object>>) map
					.get("courses");
			for (HashMap<String, Object> mp : lst) {
				record.addOffering(makeOffering(mp));
			}
			@SuppressWarnings("unchecked")
			ArrayList<String> categories = (ArrayList<String>) map
					.get("category-ids");
			@SuppressWarnings("unchecked")
			ArrayList<String> universities = (ArrayList<String>) map
					.get("university-ids");
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
	private void diffCourse(ArrayList<Change> list, CourseRec rec,
			HashMap<String, Object> map, CourseModel model) {
		// Individual fields
		String nv = (String) map.get("short_name");
		if (Change.fieldChanged(rec.getShortName(), nv))
			list.add(CourseChange.setShortName(rec, nv));
		nv = (String) map.get("name");
		if (Change.fieldChanged(rec.getName(), nv))
			list.add(CourseChange.setName(rec, nv));
		nv = (String) map.get("short_description");
		if (Change.fieldChanged(rec.getDescription(), nv))
			list.add(CourseChange.setDescription(rec, nv));
		nv = (String) map.get("instructor");
		// Old Coursera interface no longer includes instructors.
		if (nv != null && Change.fieldChanged(rec.getInstructor(), nv))
			list.add(CourseChange.setInstructor(rec, nv));
		nv = (String) map.get("social_link");
		if (Change.fieldChanged(rec.getLink(), nv))
			list.add(CourseChange.setLink(rec, nv));
		nv = (String) map.get("language");
		if (Change.fieldChanged(rec.getLanguage(), nv))
			list.add(CourseChange.setLanguage(rec, nv));

		// categories
		ArrayList<Object> incoming = (ArrayList<Object>) map
				.get("category-ids");
		HashSet<String> existing = CourseRec.idSet(rec.getCategories());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(CourseChange.setCategories(rec,
					(Collection<String>) map.get("category-ids")));
		}
		// universities
		incoming = (ArrayList<Object>) map.get("university-ids");
		existing = CourseRec.idSet(rec.getUniversities());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			list.add(CourseChange.setUniversities(rec,
					(Collection<String>) map.get("university-ids")));
		}
		// offerings
		ArrayList<HashMap<String, Object>> lst = (ArrayList<HashMap<String, Object>>) map
				.get("courses");
		HashSet<Long> newIds = new HashSet<Long>();
		for (HashMap<String, Object> o : lst)
			newIds.add((Long) o.get("id"));
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
		for (Long id : newIds) {
			HashMap<String, Object> mp = null;
			for (Object o : lst) {
				mp = (HashMap<String, Object>) o;
				if (mp.get("id").equals(id)) {
					list.add(OfferingChange.add(rec, makeOffering(mp)));
					break;
				}
			}
		}
	}

	private OffRec makeOffering(HashMap<String, Object> map) {
		Date start = getDate(map);
		return new OffRec((Long) map.get("id"), start, getDuration(map),
				getHome(map), getActive(map), getStartStr(map, start));
	}

	private void diffOffering(ArrayList<Change> list, OffRec rec,
			HashMap<String, Object> map) {
		// Use as a field parser
		boolean active = getActive(map);
		if (active != rec.isActive())
			list.add(OfferingChange.setActive(rec, active));
		Date start = getDate(map);
		if (start != null) {
			if (!start.equals(rec.getStart()))
				list.add(OfferingChange.setStart(rec, start));
		} else {
			String startStr = getStartStr(map, start);
			if (Change.fieldChanged(startStr, rec.getStartStr()))
				list.add(OfferingChange.setStartStr(rec, startStr));
		}
		long dur = getDuration(map);
		if (dur != rec.getDuration())
			list.add(OfferingChange.setDuration(rec, dur));
		String link = getHome(map);
		if (Change.fieldChanged(link, rec.getLink()))
			list.add(OfferingChange.setLink(rec, link));
	}

	public HashSet<String> getCourseLevelKeys() {
		HashSet<String> keys = new HashSet<String>();
		for (Object o : json) {
			// try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) o;
			keys.addAll(map.keySet());
			// } catch (ClassCastException e) {
			// Huh?
			// }
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
				// } catch (ClassCastException e) {
				// Huh?
			} catch (NullPointerException e) {
				System.err.println("Got a high level map in JSON "
						+ "without 'courses' key. "
						+ "Please check if Coursera's format has changed.");
			}
		}
		return keys;
	}

	private Date getDate(HashMap<String, Object> json) {
		// Some of these might be missing
		Long startDay = (Long) json.get("start_day");
		Long startMon = (Long) json.get("start_month");
		Long startYear = (Long) json.get("start_year");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (startDay == null)
			startDay = 1L;
		cal.set(Calendar.DAY_OF_MONTH, startDay.intValue());
		if (startMon == null) {
			startMon = 1L;
		}
		cal.set(Calendar.MONTH, startMon.intValue() - 1);
		if (startYear != null) {
			cal.set(Calendar.YEAR, startYear.intValue());
			return cal.getTime();
		}
		// No year => no date
		return null;
	}

	private long getDuration(HashMap<String, Object> json) {
		String durStr = (String) json.get("duration_string");
		if ("".equals(durStr))
			durStr = null;
		if (durStr != null)
			try {
				String s = durStr.trim();
				if (s.indexOf(" ") > 0)
					s = s.substring(0, s.indexOf(" "));
				if (s.indexOf("-") > 0)
					s = s.substring(0, s.indexOf("-"));
				return Integer.parseInt(s);
			} catch (Exception e) {
				System.err.println("Cannot parse duration " + durStr);
			}
		return 1;
	}

	public String getStartStr(HashMap<String, Object> json, Date start) {
		String startStr = (String) json.get("start_date_string");
		if (startStr == null && start != null)
			startStr = OffRec.dformat.format(start);
		return startStr;
	}

	private boolean getActive(HashMap<String, Object> json) {
		return (Boolean) json.get("active");
	}

	private String getHome(HashMap<String, Object> json) {
		return (String) json.get("home_link");
	}
}
