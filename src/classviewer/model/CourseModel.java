package classviewer.model;

import java.util.Collection;
import java.util.HashMap;

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
}
