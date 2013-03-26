package classviewer.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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
	/** Chached list of courses after filters have been applied */
	private ArrayList<CourseRec> filteredCourses = new ArrayList<CourseRec>();

	public CourseModel() {
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
}
