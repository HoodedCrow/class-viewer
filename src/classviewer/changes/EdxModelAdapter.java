package classviewer.changes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;
import classviewer.model.OffRec;
import classviewer.model.Source;

/**
 * EdX html is weird. I could not find an off-the-shelf parser that would
 * convert it into DOM, so here is a hackish solution.
 * 
 * Only doing additions and modifications for universities and classes.
 * 
 * @author TK
 */
public class EdxModelAdapter {
	private static SSLSocketFactory sslSocketFactory = HttpHelper
			.makeAllTrustingManager();

	/** The list of course records as read from API */
	private ArrayList<Object> json;
	private HashSet<String> universities = new HashSet<String>();
	private HashSet<String> categories = new HashSet<String>();
	/**
	 * Two-level structure of courses and offerings, with the offering stored as
	 * Json for now. The course-level key is "code,", the offerings have "guid"
	 */
	private HashMap<String, ArrayList<HashMap<String, Object>>> courses = new HashMap<String, ArrayList<HashMap<String, Object>>>();

	/**
	 * This is the main read method called from the application. It puts data
	 * into internal structure
	 */
	@SuppressWarnings("unchecked")
	public void parse(String edxUrl, boolean ignoreSSL, boolean ignoreApCourses)
			throws IOException {
		URL url = new URL(edxUrl);
		// All set up, we can get a resource through https now:
		URLConnection urlCon = url.openConnection();
		// Tell the url connection object to use our socket factory which
		// bypasses security checks
		if (ignoreSSL)
			((HttpsURLConnection) urlCon).setSSLSocketFactory(sslSocketFactory);
		InputStream stream = urlCon.getInputStream();
		InputStreamReader reader = new InputStreamReader(stream);
		this.json = (ArrayList<Object>) JsonParser.parse(reader);
		stream.close();

		// The list contains entries for both 'xseries' and 'course'. Keep only
		// courses.
		int countDeleted = 0;
		for (Iterator<Object> it = this.json.iterator(); it.hasNext();) {
			Map<String, Object> rec = (Map<String, Object>) it.next();
			if (!"course".equals(rec.get("card_type"))) {
				it.remove();
				countDeleted++;
			}
		}
		System.out.println("Dropped " + countDeleted + " records, kept "
				+ json.size());

		// Top level keys: 'organizations', 'subtitle', 'title', 'url', 'image',
		// 'card_type', 'attributes'. We already pruned by card_type.
		// 'title' and 'url' give a string. 'subtitle' has inside 'short' and
		// 'long'.
		//
		// 'organizations' is a list of maps with 'display_name', which looks
		// like
		// 'HarvardX', and 'id', which is UUID.
		//
		// 'attributes' is a map with 'start_datetime' (2014-12-30T00:00:00Z) ,
		// 'start_display_date' (e.g., Self-Paced) , 'instructors' (list of
		// maps with 'id' UUID and 'name'), 'course_key' (magic path, keep it),
		// 'course_type' ('verified', ignore for now), 'subjects' (list of maps
		// with 'id' UUID and string 'title'), 'course_number' (string, use as
		// id),
		// 'self_paced' (boolean), 'availability' ('Current'), 'course_org'
		// ('HarvardX' again, string)

		// [guid, pace, schools, subjects, start, image, l, code, types, url,
		// availability]
		universities.clear();
		categories.clear();
		courses.clear();
		for (Object o : json) {
			HashMap<String, Object> map = (HashMap<String, Object>) o;
			ArrayList<Object> list = (ArrayList<Object>) map
					.get("organizations");
			for (Object m : list) {
				String name = ((Map<String, String>) m).get("display_name");
				if (name != null && !name.isEmpty())
					universities.add(name);
			}
			Map<String, Object> attrs = (Map<String, Object>) map
					.get("attributes");
			list = (ArrayList<Object>) attrs.get("subjects");
			for (Object m : list) {
				String name = ((Map<String, String>) m).get("title");
				if (name != null && !name.isEmpty()) {
					// Will make a safe code from it later.
					categories.add(name);
				}
			}

			String courseCode = ((String) attrs.get("course_number")).trim();
			ArrayList<HashMap<String, Object>> offerings = courses
					.get(courseCode);
			if (offerings == null) {
				offerings = new ArrayList<HashMap<String, Object>>();
				courses.put(courseCode, offerings);
			}
			offerings.add(map);
		}
	}

	/**
	 * Names of schools and categories have spaces, ampersands, etc. Make it
	 * safe to appear in XML attribute.
	 */
	public static String makeIdSafe(String s) {
		s = s.replaceAll(" ", "");
		s = s.replaceAll("&", "-");
		return s;
	}

	public static String getCleanUrl(HashMap<String, Object> map) {
		String url = (String) map.get("url");
		return url.replaceAll("\\\\/", "/");
	}

	public static boolean getActiveStatus(HashMap<String, Object> map) {
		@SuppressWarnings("unchecked")
		Map<String, Object> attrs = (Map<String, Object>) map.get("attributes");
		return "Current".equals(attrs.get("availability"));
	}

	private void collectUniversityChanges(CourseModel courseModel,
			ArrayList<Change> changes) {
		// TODO: is there some API to get university descriptions? They do have
		// numeric codes in facets.
		for (String u : universities) {
			String code = makeIdSafe(u);
			if (courseModel.getUniversity(Source.EDX, code) == null)
				changes.add(DescChange.add(DescChange.UNIVERSITY, Source.EDX,
						code, u, null));
		}
		HashSet<String> newIds = new HashSet<String>();
		for (String s : this.universities)
			newIds.add(makeIdSafe(s));
		for (DescRec rec : courseModel.getUniversities(Source.EDX)) {
			if (!newIds.contains(rec.getId())) {
				changes.add(DescChange.delete(DescChange.UNIVERSITY, rec));
			} else {
				// TODO Once we get description for universities, diff them.
			}
			newIds.remove(rec.getId());
		}
	}

	private void collectCategoryChanges(CourseModel courseModel,
			ArrayList<Change> changes) {
		// TODO: category names are long, and they are mapped to numeric codes
		// in facets. Something to use in description?
		for (String c : categories) {
			String code = makeIdSafe(c);
			if (courseModel.getCategory(Source.EDX, code) == null)
				changes.add(DescChange.add(DescChange.CATEGORY, Source.EDX,
						code, c, null));
		}
		HashSet<String> newIds = new HashSet<String>();
		for (String s : this.categories)
			newIds.add(makeIdSafe(s));
		for (DescRec rec : courseModel.getCategories(Source.EDX)) {
			if (!newIds.contains(rec.getId())) {
				changes.add(DescChange.delete(DescChange.CATEGORY, rec));
			} else {
				// TODO Once have descriptions, do diff.
			}
			newIds.remove(rec.getId());
		}
	}

	/**
	 * Compare the internal structure produced by the parse method to the given
	 * model and return the set of differences
	 */
	public ArrayList<Change> collectChanges(CourseModel courseModel,
			int tooOldInDays) {
		// Build a date before which we do not remove offerings
		Date tooOld = new Date(new Date().getTime() - 24 * 3600000l
				* tooOldInDays);
		System.out.println("Will keep all offerings older than " + tooOldInDays
				+ " days: " + tooOld);

		ArrayList<Change> changes = new ArrayList<Change>();
		collectUniversityChanges(courseModel, changes);
		collectCategoryChanges(courseModel, changes);

		// Short codes are used as keys. We hope they don't collide across
		// universities, but who knows what EdX uses inside.
		for (String code : courses.keySet()) {
			CourseRec oldRec = courseModel.getClassById(code, Source.EDX);
			if (oldRec == null)
				oldRec = courseModel.getClassByShortName(code, Source.EDX);
			ArrayList<HashMap<String, Object>> course = courses.get(code);
			if (oldRec != null) {
				diffCourse(course, oldRec, changes, courseModel, tooOld);
			} else {
				HashMap<String, Object> map = course.get(0);
				// Id and short name are both code.
				String name = getLongName(map);
				String dsc = null; // Comes from a separate file. Not using for
									// now.
				String instructor = null; // TODO
				String language = "en"; // TODO in a separate file.
				String link = null; // Will use per offering urls.
				CourseRec record = new CourseRec(Source.EDX, code, code, name,
						dsc, instructor, link, language, getSelfStudy(map));
				for (HashMap<String, Object> m : course) {
					record.addOffering(makeOffering(
							courseModel.makeNewOfferingId(Source.EDX), m));
				}
				ArrayList<String> categories = getCategoryCodes(map);
				ArrayList<String> universities = getUniversityCodes(map);
				changes.add(CourseChange.add(record, categories, universities));
			}
		}

		// TODO deleted courses.
		return changes;
	}

	private static String getLongName(HashMap<String, Object> map) {
		return (String) map.get("title");
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<String> getCategoryCodes(
			HashMap<String, Object> map) {
		ArrayList<String> list = new ArrayList<String>();
		Map<String, Object> attrs = (Map<String, Object>) map.get("attributes");
		for (Object m : (ArrayList<Object>) attrs.get("subjects")) {
			String name = ((Map<String, String>) m).get("title");
			if (name != null && !name.isEmpty())
				list.add(makeIdSafe(name));
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<String> getUniversityCodes(
			HashMap<String, Object> map) {
		ArrayList<String> list = new ArrayList<String>();
		for (Object m : (ArrayList<Object>) map.get("organizations")) {
			String name = ((Map<String, String>) m).get("display_name");
			if (name != null && !name.isEmpty())
				list.add(makeIdSafe(name));
		}
		return list;
	}

	private static String getKey(HashMap<String, Object> map) {
		@SuppressWarnings("unchecked")
		Map<String, Object> attrs = (Map<String, Object>) map.get("attributes");
		return (String) attrs.get("course_key");
	}

	private static Date getStartDate(HashMap<String, Object> map) {
		@SuppressWarnings("unchecked")
		Map<String, Object> attrs = (Map<String, Object>) map.get("attributes");
		String string = (String) attrs.get("start_datetime");
		int index = string.indexOf("T");
		if (index > 0)
			string = string.substring(0, index);
		return HttpHelper.parseDate(string);
	}

	private static boolean getSelfStudy(HashMap<String, Object> map) {
		@SuppressWarnings("unchecked")
		Map<String, Object> attrs = (Map<String, Object>) map.get("attributes");
		return Boolean.TRUE.equals(attrs.get("self_paced"));
	}

	private void diffCourse(ArrayList<HashMap<String, Object>> classJson,
			CourseRec oldRec, ArrayList<Change> changes, CourseModel model,
			Date tooOld) {
		HashMap<String, Object> first = classJson.get(0);

		// Long name.
		String longName = getLongName(first);
		String oldLongName = oldRec.getName();
		if (oldLongName == null && longName != null || oldLongName != null
				&& !oldLongName.equals(longName))
			changes.add(CourseChange.setName(oldRec, longName));
		// No description for now.

		// Categories.
		ArrayList<String> incoming = getCategoryCodes(first);
		HashSet<String> existing = CourseRec.idSet(oldRec.getCategories());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			changes.add(CourseChange.setCategories(oldRec, incoming));
		}

		// Universities
		incoming = getUniversityCodes(first);
		existing = CourseRec.idSet(oldRec.getUniversities());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			changes.add(CourseChange.setUniversities(oldRec, incoming));
		}
		ArrayList<OffRec> oldOffs = new ArrayList<OffRec>(oldRec.getOfferings());
		for (HashMap<String, Object> newOff : classJson) {
			String key = getKey(newOff);
			Date newDate = getStartDate(newOff);
			// Find it by key or, for now, date.
			OffRec oldOff = null;
			for (OffRec o : oldOffs)
				if (key.equals(o.getKey()) || o.getStart() != null
						&& o.getStart().equals(newDate)) {
					if (o.getKey() == null) {
						System.out
								.println("Setting key to existing offering found by date: "
										+ key);
						o.setKey(key);
					}
					oldOff = o;
					break;
				}
			if (oldOff != null) {
				oldOffs.remove(oldOff); // Accounted for
				diffOffering(oldRec, oldOff, newOff, changes);
			} else {
				changes.add(OfferingChange.add(
						oldRec,
						makeOffering(model.makeNewOfferingId(Source.EDX),
								newOff)));
			}
		}
		// Remove old offerings if any left.
		for (OffRec o : oldOffs) {
			if (o.getStart() == null || o.getStart().after(tooOld)) {
				changes.add(OfferingChange.delete(o));
			}
		}
	}

	private OffRec makeOffering(long id, HashMap<String, Object> map) {
		String startStr = null; // Will not use anymore.
		Date start = getStartDate(map);
		int duration = 1; // This will come later from a separate load.
		String home = getCleanUrl(map);
		boolean active = getActiveStatus(map);
		return new OffRec(id, start, duration, home, active, startStr)
				.setKey(getKey(map));
	}

	private void diffOffering(CourseRec oldRec, OffRec oldOff,
			HashMap<String, Object> newOff, ArrayList<Change> changes) {
		// Start date.
		Date newDate = getStartDate(newOff);
		if (oldOff.getStart() == null && newDate != null
				|| oldOff.getStart() != null
				&& !oldOff.getStart().equals(newDate)) {
			changes.add(OfferingChange.setStart(oldOff, newDate));
		}

		// URL. With some clean up.
		String newUrl = getCleanUrl(newOff);
		if (Change.fieldChanged(newUrl, oldOff.getLink())) {
			changes.add(OfferingChange.setLink(oldOff, newUrl));
		}

		// Active from availability.
		Boolean active = getActiveStatus(newOff);
		if (oldOff.isActive() != active) {
			changes.add(OfferingChange.setActive(oldOff, active));
		}
	}

	public boolean loadClassDuration(OffRec off, boolean ignoreSSL)
			throws IOException {
		if (off.getKey() == null)
			return false;
		// https://www.edx.org/api/catalog/v2/courses/course-v1:DelftX+TP101x+3T2015
		// "https://www.edx.org/api/catalog/v2/courses/course-v1:";
		String newUrl = "https://www.edx.org/api/catalog/v2/courses/"
				+ off.getKey();
		URL url = new URL(newUrl);
		// All set up, we can get a resource through https now:
		URLConnection urlCon = url.openConnection();
		// Tell the url connection object to use our socket factory which
		// bypasses security checks
		if (ignoreSSL)
			((HttpsURLConnection) urlCon).setSSLSocketFactory(sslSocketFactory);
		InputStream stream = urlCon.getInputStream();
		InputStreamReader reader = new InputStreamReader(stream);

		StringBuffer buffer = HttpHelper.readIntoBuffer(reader);
		stream.close();

		Object data = JsonParser.parse(new StringReader(buffer.toString()));
		try {
			@SuppressWarnings("unchecked")
			String value = (String) ((Map<String, Object>) data).get("length");
			value = value.trim();
			int end = value.indexOf(' ');
			if (end >= 0)
				value = value.substring(0, end);
			int weeks = Integer.parseInt(value);
			off.setDuration(weeks);
			return true;
		} catch (Exception e) {
			System.out.println("Cannot get course length from " + newUrl + ": "
					+ e);
			return false;
		}
	}
}
