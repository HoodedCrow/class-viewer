package classviewer.changes;

import java.io.IOException;
import java.util.ArrayList;

import classviewer.model.CourseModel;

public interface CourseraModelAdapter {
	public void load(String courseraUrl) throws IOException;

	public ArrayList<Change> collectChanges(CourseModel model);
}
