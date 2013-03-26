package classviewer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * Layout manager for the main frame. Eventually may have multiple of these with
 * a common interface/abstract parent. For now very simplified and brute-force.
 * 
 * @author TK
 */
public class MainWindowLayout implements LayoutManager {

	private CourseFilterFrame courseFilterFrame;
	private CourseListFrame courseListFrame;
	private CalendarFrame calendarFrame;
	private DetailsFrame detailsFrame;
	private ChangesFrame changesFrame;

	public MainWindowLayout(CourseFilterFrame courseFilterFrame,
			CourseListFrame courseListFrame, CalendarFrame calendarFrame,
			DetailsFrame detailsFrame, ChangesFrame changesFrame) {
		this.courseFilterFrame = courseFilterFrame;
		this.courseListFrame = courseListFrame;
		this.calendarFrame = calendarFrame;
		this.detailsFrame = detailsFrame;
		this.changesFrame = changesFrame;
	}

	@Override
	public void layoutContainer(Container parent) {
		// TODO How do we figure out the height consumed by the bottom row of icons?
		int pW = parent.getWidth();
		int pH = parent.getHeight() - 32;
		
		// Width of the filter frame
		int x1 = 300;
		// Width of the course list frame + x1
		int x2 = 400 + x1;
		// Start of the details/changes frames
		int y1 = pH - 300;

		// Assume these two are visible
		courseFilterFrame.setLocation(0, 0);
		courseListFrame.setLocation(x1, 0);

		if (detailsFrame.isVisible()) {
			courseFilterFrame.setSize(x1, y1);
			courseListFrame.setSize(x2 - x1, y1);
			detailsFrame.setLocation(0, y1);
			detailsFrame.setSize(x2, pH - y1);
		} else {
			courseFilterFrame.setSize(x1, pH);
			courseListFrame.setSize(x2 - x1, pH);
		}

		// Assuming calendar is visible
		calendarFrame.setLocation(x2, 0);
		if (changesFrame.isVisible()) {
			calendarFrame.setSize(pW - x2, y1);
			changesFrame.setLocation(x2, y1);
			changesFrame.setSize(pW - x2, pH - y1);
		} else {
			calendarFrame.setSize(pW - x2, pH);
		}
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
	}

	@Override
	public void removeLayoutComponent(Component comp) {
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return null;
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return null;
	}
}
