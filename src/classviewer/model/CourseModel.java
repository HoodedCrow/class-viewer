package classviewer.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import classviewer.Settings;
import classviewer.filters.CategoryCourseFilter;
import classviewer.filters.CourseFilter;
import classviewer.filters.UniversityCourseFilter;

/**
 * Main model file. Contains a bunch of hashes for universities, languages,
 * courses, offerings, etc.
 * 
 * @author TK
 */
public class CourseModel {
	private HashMap<String, DescRec> categories = new HashMap<String, DescRec>();
	private HashMap<String, DescRec> universities = new HashMap<String, DescRec>();
	private HashMap<Integer, CourseRec> courses = new HashMap<Integer, CourseRec>();
	private ArrayList<CourseFilter> filters = new ArrayList<CourseFilter>();
	private ArrayList<CourseModelListener> listeners = new ArrayList<CourseModelListener>();
	/** Cached list of courses after filters have been applied */
	private ArrayList<CourseRec> filteredCourses = new ArrayList<CourseRec>();
	private Settings settings;

	public CourseModel(Settings settings) {
		this.settings = settings;
		filters.add(new CategoryCourseFilter(this));
		filters.add(new UniversityCourseFilter(this));
	}

	public void addCategory(DescRec desc) {
		if (desc == null)
			return;
		if (categories.containsKey(desc.getId()))
			System.err.println("Ignoring duplicate category key "
					+ desc.getId());
		categories.put(desc.getId(), desc);
	}

	public void addUniversity(DescRec desc) {
		if (desc == null)
			return;
		if (universities.containsKey(desc.getId()))
			System.err.println("Ignoring duplicate university key "
					+ desc.getId());
		universities.put(desc.getId(), desc);
	}

	public void addCourse(CourseRec course) {
		if (courses.containsKey(course.getId()))
			System.err
					.println("Ignoring duplicate course id " + course.getId());
		courses.put(course.getId(), course);
	}

	public DescRec getCategory(String id) {
		return categories.get(id);
	}

	public DescRec getUniversity(String id) {
		return universities.get(id);
	}

	public Collection<DescRec> getCategories() {
		return categories.values();
	}

	public Collection<DescRec> getUniversities() {
		return universities.values();
	}

	public Collection<CourseRec> getCourses() {
		return courses.values();
	}

	public ArrayList<CourseFilter> getFilters() {
		return filters;
	}

	public void fireModelReloaded() {
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
		for (CourseRec rec : courses.values()) {
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

	public CourseRec getClassById(int id) {
		return courses.get(id);
	}

	public void saveStatusFile() throws IOException {
		// System.out.println("Save status file now");
		File file = settings.getStatusFile();
		FileWriter writer = new FileWriter(file);
		StatusFileModelAdapter adap = new StatusFileModelAdapter();
		ArrayList<CourseRec> all = new ArrayList<CourseRec>(courses.values());
		Collections.sort(all, new CourseById());
		adap.saveStatuses(writer, all);
		writer.close();
	}

	private class CourseById implements Comparator<CourseRec> {
		@Override
		public int compare(CourseRec o1, CourseRec o2) {
			return Integer.compare(o1.getId(), o2.getId());
		}
	}
}
