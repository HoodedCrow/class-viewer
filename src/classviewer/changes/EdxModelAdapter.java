package classviewer.changes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

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

		// Keys for each record:
		// [guid, pace, schools, subjects, start, image, l, code, types, url,
		// availability]
		universities.clear();
		categories.clear();
		courses.clear();
		for (Object o : json) {
			HashMap<String, Object> map = (HashMap<String, Object>) o;
			ArrayList<String> list = (ArrayList<String>) map.get("schools");
			while (list.remove(""));
			universities.addAll(list);
			list = (ArrayList<String>) map.get("subjects");
			// EdX has empty subjects since they moved Verified into "types"
			while (list.remove(""));
			categories.addAll(list);
			String courseCode = getCleanCode(map);
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

	public static String makeCategoryId(String s) {
		return makeIdSafe(s);
	}

	public static String getCleanUrl(HashMap<String, Object> map) {
		String url = (String) map.get("url");
		return url.replaceAll("\\\\/", "/");
	}

	public static String getCleanCode(HashMap<String, Object> map) {
		String code = (String) map.get("code");
		return code.trim();
	}

	public static boolean getActiveStatus(HashMap<String, Object> map) {
		return "Current".equals(map.get("availability"));
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
			String code = makeCategoryId(c);
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
	@SuppressWarnings("unchecked")
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
			CourseRec oldRec = courseModel.getClassByShortName(code, Source.EDX);
			ArrayList<HashMap<String, Object>> course = courses.get(code);
			// Fishing here. Should not be necessary once everything converts to
			// guids on the EdX side.
			if (oldRec == null) {
				HashMap<String, Object> first = course.get(0);
				String name = (String) first.get("l");
				String uni = ((ArrayList<String>) first.get("schools")).get(0);
				oldRec = courseModel.getClassByLongNameAndUni(name, uni, Source.EDX);
				if (oldRec != null) {
					System.out.println("Code mismatch ["
							+ oldRec.getShortName() + "] != [" + code + "]");
					// oldRec.setShortName(code);
				}
			}
			if (oldRec != null) {
				diffCourse(course, oldRec, changes, courseModel, tooOld);
			} else {
				HashMap<String, Object> map = course.get(0);
				String id = String.valueOf(map.get("guid")); // TODO Remove?
																// model.getNewNegativeId();
				String short_name = EdxModelAdapter.getCleanCode(map);
				String name = (String) map.get("l");
				String dsc = null; // TODO
				String instructor = null; // TODO (ArrayList<String>) map.get("staff");
				String language = "en"; // TODO until further notice. "languages"
				String link = null; // TODO "url"
				CourseRec record = new CourseRec(Source.EDX, id, short_name,
					name, dsc, instructor, link, language, false); // TODO until further notice
				for (HashMap<String, Object> m : course) {
					record.addOffering(makeOffering(m));
				}
				ArrayList<String> categories = new ArrayList<String>();
				for (String s : (ArrayList<String>) map.get("subjects")) {
					categories.add(makeCategoryId(s));
				}
				ArrayList<String> universities = new ArrayList<String>();
				for (String s : (ArrayList<String>) map.get("schools")) {
					universities.add(makeIdSafe(s));
				}
				changes.add(CourseChange.add(record, categories, universities));
			}
		}

		// TODO deleted courses.
		return changes;
	}

	@SuppressWarnings("unchecked")
	private void diffCourse(ArrayList<HashMap<String, Object>> offerings,
			CourseRec oldRec, ArrayList<Change> changes, CourseModel model,
			Date tooOld) {
		// From the first record pull universities, categories, and long name.
		// There is no description here. Maybe in another query?
		HashMap<String, Object> first = offerings.get(0);
		String longName = (String) first.get("l");

		// Long name.
		String oldLongName = oldRec.getName();
		if (oldLongName == null && longName != null || oldLongName != null
				&& !oldLongName.equals(longName))
			changes.add(CourseChange.setName(oldRec, longName));
		// No description for now.

		// Categories.
		ArrayList<String> incoming = new ArrayList<String>();
		for (String s : (ArrayList<String>) first.get("subjects"))
			incoming.add(makeCategoryId(s));
		HashSet<String> existing = CourseRec.idSet(oldRec.getCategories());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			changes.add(CourseChange.setCategories(oldRec, incoming));
		}

		// Universities
		incoming = new ArrayList<String>();
		for (String s : (ArrayList<String>) first.get("schools"))
			incoming.add(makeIdSafe(s));
		existing = CourseRec.idSet(oldRec.getUniversities());
		if (incoming.size() != existing.size()
				|| !existing.containsAll(incoming)) {
			changes.add(CourseChange.setUniversities(oldRec, incoming));
		}

		// EdX offerings now have guids, finally! We will flip them to negative
		// values until TODO source flags are introduced.
		ArrayList<OffRec> oldOffs = new ArrayList<OffRec>(oldRec.getOfferings());
		for (HashMap<String, Object> newOff : offerings) {
			long newId = (Long) newOff.get("guid");
			Date newDate = HttpHelper.parseDate((String) newOff.get("start"));
			// Find it by id or, for now, date.
			OffRec oldOff = null;
			for (OffRec o : oldOffs)
				if (o.getId() == newId || o.getStart() != null
						&& o.getStart().equals(newDate)) {
					// Quietly change id. TODO temporary!
					if (o.getId() != newId) {
						System.out.println("Changing offering id " + o.getId()
								+ " to " + newId);
						o.updateId((int) newId);
					}
					oldOff = o;
					break;
				}
			if (oldOff != null) {
				oldOffs.remove(oldOff); // Accounted for
				diffOffering(oldRec, oldOff, newOff, changes);
			} else {
				changes.add(OfferingChange.add(oldRec, makeOffering(newOff)));
			}
		}
		// Remove old offerings if any left.
		for (OffRec o : oldOffs) {
			if (o.getStart() == null || o.getStart().after(tooOld)) {
				changes.add(OfferingChange.delete(o));
			}
		}
	}

	private OffRec makeOffering(HashMap<String, Object> map) {
		long id = (Long) map.get("guid");
		String startStr = (String) map.get("start");
		Date start = HttpHelper.parseDate(startStr);
		int duration = 1; // TODO
		String home = getCleanUrl(map);
		boolean active = getActiveStatus(map);
		return new OffRec(id, start, duration, home, active, startStr);
	}

	private void diffOffering(CourseRec oldRec, OffRec oldOff,
			HashMap<String, Object> newOff, ArrayList<Change> changes) {
		// Start date. TODO self-paced.
		Date newDate = HttpHelper.parseDate((String) newOff.get("start"));
		if (oldOff.getStart() == null && newDate != null
				|| oldOff.getStart() != null
				&& !oldOff.getStart().equals(newDate)) {
			// TODO? Save as a string.
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

	public String extractEndDate(String baseUrl, String home) {
		final String tag = "<span class=\"final-date\">";

		String addr = baseUrl + home;
		try {
			URL url = new URL(addr);
			InputStream stream = url.openStream();
			InputStreamReader reader = new InputStreamReader(stream);

			StringBuffer buffer = HttpHelper.readIntoBuffer(reader);
			stream.close();

			int idx = buffer.indexOf(tag);
			if (idx < 0)
				return null;
			int end = buffer.indexOf("</span>", idx);
			return buffer.substring(idx + tag.length(), end).trim();
		} catch (Exception e) {
			System.err.println("Cannot get end date from " + addr);
		}
		return null;
	}

	public boolean loadClassDuration(OffRec off, boolean ignoreSSL)
			throws IOException {
		String tail = off.getLink();
		if (tail == null)
			return false;
		URL url = new URL(tail);
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

		// We are looking for the number of weeks. This code is specific to the
		// EdX format
		final String startLabel = "Course Length:";
		int start = buffer.indexOf(startLabel);
		if (start < 0) {
			return false;
		}
		start += startLabel.length();
		int end = buffer.indexOf(" week", start);
		if (end < 0) {
			return false;
		}

		// This slice should end with the number we are looking for
		String slice = buffer.substring(start, end).trim();
		for (start = slice.length() - 1; start >= 0
				&& Character.isDigit(slice.charAt(start)); start--)
			continue;
		int weeks = Integer.parseInt(slice.substring(start + 1));

		off.setDuration(weeks);
		return true;
	}
}
