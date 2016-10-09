package classviewer;

import classviewer.model.CourseRec;
import classviewer.model.OffRec;

/** Listen to things being clicked in the ChangesFrame. */
public interface ChangeSelectionListener {
	public void offeringChangeSelected(OffRec offering);

	public void courseChangeSelected(CourseRec course);
}
