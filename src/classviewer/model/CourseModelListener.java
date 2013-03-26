package classviewer.model;

public interface CourseModelListener {
	void courseStatusChanged(CourseRec course);
	void modelUpdated();
	void filtersUpdated();
}
