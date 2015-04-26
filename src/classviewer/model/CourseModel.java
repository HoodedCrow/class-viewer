package classviewer.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import classviewer.Settings;
import classviewer.filters.CategoryCourseFilter;
import classviewer.filters.CourseFilter;
import classviewer.filters.LanguageCourseFilter;
import classviewer.filters.SelfPacedCourseFilter;
import classviewer.filters.SourceCourseFilter;
import classviewer.filters.StatusCourseFilter;
import classviewer.filters.UniversityCourseFilter;

/**
 * Main model file. Contains a bunch of hashes for universities, languages,
 * courses, offerings, etc.
 * 
 * @author TK
 */
public class CourseModel {
	class Submodel {
		private HashMap<String, DescRec> categories = new HashMap<String, DescRec>();
		private HashMap<String, DescRec> universities = new HashMap<String, DescRec>();
		private HashMap<Long, CourseRec> courses = new HashMap<Long, CourseRec>();
		/** Smallest (most negative) id ever assigned. */
		private long smallestId = 0;

		private void addCategory(DescRec desc) {
			if (categories.containsKey(desc.getId()))
				System.err.println("Ignoring duplicate category key "
						+ desc.getId());
			categories.put(desc.getId(), desc);
		}

		private void addUniversity(DescRec desc) {
			if (universities.containsKey(desc.getId()))
				System.err.println("Ignoring duplicate university key "
						+ desc.getId());
			universities.put(desc.getId(), desc);
		}

		public void addCourse(CourseRec course) {
			if (courses.containsKey(course.getId()))
				System.err.println("Ignoring duplicate course id "
						+ course.getId());
			courses.put(course.getId(), course);
		}

		/**
		 * Get the smallest int value mentioned in class or offering ids. Assume
		 * this method will not be called often.
		 */
		private void updateSmallestId() {
			long value = 0; // default
			for (CourseRec cr : courses.values()) {
				value = Math.min(value, cr.getId());
				for (OffRec or : cr.getOfferings())
					value = Math.min(value, or.getId());
			}
			this.smallestId = value;
		}
	}

	/**
	 * Categories, universities, courses per source. Indexed by ordinal of the
	 * source
	 */
	private Submodel[] submodel = new Submodel[Source.values().length];
	private ArrayList<CourseFilter> filters = new ArrayList<CourseFilter>();
	private ArrayList<CourseModelListener> listeners = new ArrayList<CourseModelListener>();
	/** Cached list of courses after filters have been applied */
	private ArrayList<CourseRec> filteredCourses = new ArrayList<CourseRec>();
	private Settings settings;

	public CourseModel(Settings settings) {
		this.settings = settings;
		filters.add(new SourceCourseFilter(this));
		filters.add(new StatusCourseFilter(this));
		filters.add(new CategoryCourseFilter(this));
		filters.add(new UniversityCourseFilter(this));
		filters.add(new SelfPacedCourseFilter(this));
		filters.add(new LanguageCourseFilter(this));

		for (int i = 0; i < submodel.length; i++)
			submodel[i] = new Submodel();
	}

	public void addCategory(DescRec desc) {
		if (desc != null)
			submodel[desc.getSource().ordinal()].addCategory(desc);
	}

	public void addUniversity(DescRec desc) {
		if (desc != null)
			submodel[desc.getSource().ordinal()].addUniversity(desc);
	}

	public DescRec removeUniversity(Source source, String id) {
		return submodel[source.ordinal()].universities.remove(id);
	}

	public void addCourse(CourseRec course) {
		submodel[course.getSource().ordinal()].addCourse(course);
	}

	public CourseRec getCourse(Source source, Long id) {
		return submodel[source.ordinal()].courses.get(id);
	}

	public CourseRec removeCourse(Source source, Long id) {
		return submodel[source.ordinal()].courses.remove(id);
	}

	public DescRec getCategory(Source source, String id) {
		// TODO Temporary until all is converted
		DescRec res = submodel[source.ordinal()].categories.get(id);
		if (res == null) {
			res = submodel[Source.MISSING.ordinal()].categories.get(id);
			if (res == null)
				return null;
			System.out.println("Changing source of " + res + " to " + source);
			submodel[Source.MISSING.ordinal()].categories.remove(id);
			res.source = source;
			submodel[source.ordinal()].addCategory(res);
		}
		return res;
	}

	public DescRec removeCategory(Source source, String id) {
		return submodel[source.ordinal()].categories.remove(id);
	}

	public DescRec getUniversity(Source source, String id) {
		// TODO Temporary until all is converted
		DescRec res = submodel[source.ordinal()].universities.get(id);
		if (res == null) {
			res = submodel[Source.MISSING.ordinal()].universities.get(id);
			if (res == null)
				return null;
			System.out.println("Changing source of " + res + " to " + source);
			submodel[Source.MISSING.ordinal()].universities.remove(id);
			res.source = source;
			submodel[source.ordinal()].addUniversity(res);
		}
		return res;
	}

	public Collection<DescRec> getCategories(Source source) {
		return submodel[source.ordinal()].categories.values();
	}

	public Collection<DescRec> getUniversities(Source source) {
		return submodel[source.ordinal()].universities.values();
	}

	public Collection<CourseRec> getCourses(Source source) {
		return submodel[source.ordinal()].courses.values();
	}

	public ArrayList<CourseFilter> getFilters() {
		return filters;
	}

	public void fireModelReloaded() {
		for (Source source : Source.values())
			submodel[source.ordinal()].updateSmallestId(); // TODO Remove?
		applyCourseFilters();
		for (CourseModelListener lnr : listeners)
			lnr.modelUpdated();
	}

	public void fireFiltersChanged(CourseFilter filter) {
		applyCourseFilters();
		for (CourseModelListener lnr : listeners)
			lnr.filtersUpdated();
	}

	/** Course or one of it's offerings changed status */
	public void fireCourseStatusChanged(CourseRec course) {
		applyCourseFilters();
		for (CourseModelListener lnr : listeners)
			lnr.courseStatusChanged(course);
	}

	public void addListener(CourseModelListener lnr) {
		this.listeners.add(lnr);
	}

	public void removeListener(CourseModelListener lnr) {
		this.listeners.remove(lnr);
	}

	private void applyCourseFilters() {
		filteredCourses.clear();
		for (Source source : Source.values())
			for (CourseRec rec : submodel[source.ordinal()].courses.values()) {
				boolean passed = true;
				for (int i = 0; i < filters.size() && passed; i++)
					passed = filters.get(i).accept(rec);
				if (passed)
					filteredCourses.add(rec);
			}
	}

	public ArrayList<CourseRec> getFilteredCourses() {
		return filteredCourses;
	}

	public CourseRec getClassById(long id, Source source) {
		// TODO Temporary until all is converted
		CourseRec res = submodel[source.ordinal()].courses.get(id);
		if (res == null)
			for (Submodel sm : submodel) {
				res = sm.courses.get(id);
				if (res != null)
					break;
			}
		return res;
	}

	/**
	 * Locate the class by short name AKA string id. This is used, for example,
	 * when EdX data is imported.
	 */
	public CourseRec getClassByShortName(String courseId, Source source) {
		for (CourseRec cr : submodel[source.ordinal()].courses.values()) {
			if (courseId.equals(cr.getShortName()))
				return cr;
		}
		return null;
	}

	public CourseRec getClassByLongNameAndUni(String name, String uni,
			Source source) {
		for (CourseRec cr : submodel[source.ordinal()].courses.values()) {
			if (name.equals(cr.getName())) {
				for (DescRec dr : cr.getUniversities())
					if (uni.equals(dr.getId()))
						return cr;
			}
		}
		return null;
	}

	public HashSet<String> getLanguages() {
		HashSet<String> res = new HashSet<String>();
		for (Source source : Source.values())
			for (CourseRec r : submodel[source.ordinal()].courses.values())
				res.add(r.getLanguage());
		// Clean up possible NULL and ""?
		return res;
	}

	public void saveStatusFile() throws IOException {
		File file = settings.getStatusFile();
		FileWriter writer = new FileWriter(file);
		StatusFileModelAdapter adap = new StatusFileModelAdapter();
		ArrayList<CourseRec> all = new ArrayList<CourseRec>();
		for (Source source : Source.values())
			all.addAll(submodel[source.ordinal()].courses.values());
		Collections.sort(all, new CourseBySourceAndId());
		adap.saveStatuses(writer, all);
		writer.close();
	}

	public void saveModelFile() throws IOException {
		File file = settings.getStaticFile();
		FileOutputStream output = new FileOutputStream(file);
		XmlModelAdapter xml = new XmlModelAdapter();
		xml.writeModel(output, this);
		output.close();
	}

	// TODO Delete
	// /** Generate a brand new negative ID for use for EdX and such */
	// public int getNewNegativeId() {
	// this.smallestId--;
	// return this.smallestId;
	// }

	private class CourseBySourceAndId implements Comparator<CourseRec> {
		@Override
		public int compare(CourseRec o1, CourseRec o2) {
			int bySource = Integer.compare(o1.getSource().ordinal(), o2
					.getSource().ordinal());
			if (bySource == 0)
				return Long.compare(o1.getId(), o2.getId());
			return bySource;
		}
	}

	public Settings getSettings() {
		return this.settings;
	}
}
