package classviewer;

import java.awt.Container;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;

/**
 * Layout manager for the main frame. Eventually may have multiple of these with
 * a common interface/abstract parent. For now very simplified and brute-force.
 * 
 * Base on the default manager and hook up our resizer everywhere.
 * 
 * @author TK
 */
public class MainWindowLayout extends DefaultDesktopManager {

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

	protected void updateOtherFramesBasedOn(JComponent f) {
		Container parent = this.calendarFrame.getParent();
		int pW = parent.getWidth();
		int pH = parent.getHeight();

		// x1 is the start of course list, x2 is the start of calendar/changes
		// y1 is the start of details, y2 is the start of changes
		int x1 = 0, x2 = 0, y1 = 0, y2 = 0;

		if (this.courseFilterFrame.isVisible()) {
			if (f == this.courseFilterFrame) {
				x1 = this.courseFilterFrame.getWidth();
			} else if (this.courseListFrame.isVisible()) {
				x1 = this.courseListFrame.getX();
				if (x1 < this.courseFilterFrame.getMinimumSize().width)
					x1 = this.courseFilterFrame.getMinimumSize().width;
			} else {
				x1 = this.courseFilterFrame.getWidth();
			}
			if (x1 < this.courseFilterFrame.getMinimumSize().width)
				x1 = this.courseFilterFrame.getMinimumSize().width;
		}

		if (this.courseListFrame.isVisible()) {
			if (f == this.courseListFrame) {
				x2 = x1 + this.courseListFrame.getWidth();
			} else if (this.calendarFrame.isVisible()) {
				x2 = this.calendarFrame.getX();
			} else if (this.changesFrame.isVisible()) {
				x2 = this.changesFrame.getX();
			} else {
				x2 = x1 + this.courseListFrame.getWidth();
			}
			if (x2 - x1 < this.courseListFrame.getMinimumSize().width)
				x2 = x1 + this.courseListFrame.getMinimumSize().width;
		} else {
			x2 = x1;
		}
		if (this.detailsFrame.isVisible()
				&& x2 < this.detailsFrame.getMinimumSize().width) {
			x2 = this.detailsFrame.getMinimumSize().width;
		}

		if (this.detailsFrame.isVisible()) {
			if (f == this.courseFilterFrame) {
				y1 = this.courseFilterFrame.getHeight();
			} else if (f == this.courseListFrame) {
				y1 = this.courseListFrame.getHeight();
			} else if (!this.courseFilterFrame.isVisible()
					&& !this.courseListFrame.isVisible()) {
				y1 = 0;
			} else {
				y1 = pH - this.detailsFrame.getHeight();
			}
			if (this.courseFilterFrame.isVisible()
					&& y1 < this.courseFilterFrame.getMinimumSize().height)
				y1 = this.courseFilterFrame.getMinimumSize().height;
			if (this.courseListFrame.isVisible()
					&& y1 < this.courseListFrame.getMinimumSize().height)
				y1 = this.courseListFrame.getMinimumSize().height;
			if (pH - y1 < this.detailsFrame.getMinimumSize().height)
				y1 = pH - this.detailsFrame.getMinimumSize().height;
		} else {
			y1 = pH;
		}

		if (this.changesFrame.isVisible()) {
			if (f == this.calendarFrame) {
				y2 = this.calendarFrame.getHeight();
			} else if (!this.calendarFrame.isVisible()) {
				y2 = 0;
			} else {
				y2 = pH - this.changesFrame.getHeight();
			}
			if (this.calendarFrame.isVisible()
					&& y2 < this.calendarFrame.getMinimumSize().height)
				y2 = this.calendarFrame.getMinimumSize().height;
			if (pH - y2 < this.changesFrame.getMinimumSize().getHeight())
				y2 = pH - (int) this.changesFrame.getMinimumSize().getHeight();
		} else {
			y2 = pH;
		}

		// Set numbers
		this.courseFilterFrame.setBounds(0, 0, x1, y1);
		this.courseListFrame.setBounds(x1, 0, x2 - x1, y1);
		this.detailsFrame.setBounds(0, y1, x2, pH - y1);
		this.calendarFrame.setBounds(x2, 0, pW - x2, y2);
		this.changesFrame.setBounds(x2, y2, pW - x2, pH - y2);
	}

	/**
	 * Normally this method will not be called. If it is, it try to determine
	 * the appropriate parent from the desktopIcon of the frame. Will remove the
	 * desktopIcon from its parent if it successfully adds the frame.
	 */
	public void openFrame(JInternalFrame f) {
		super.openFrame(f);
		updateOtherFramesBasedOn(f);
	}

	/**
	 * Removes the frame, and, if necessary, the <code>desktopIcon</code>, from
	 * its parent.
	 * 
	 * @param f
	 *            the <code>JInternalFrame</code> to be removed
	 */
	public void closeFrame(JInternalFrame f) {
		super.closeFrame(f);
		updateOtherFramesBasedOn(null);
	}

	/**
	 * Resizes the frame to fill its parents bounds.
	 * 
	 * @param f
	 *            the frame to be resized
	 */
	public void maximizeFrame(JInternalFrame f) {
		super.maximizeFrame(f);
		updateOtherFramesBasedOn(f);
	}

	/**
	 * Restores the frame back to its size and position prior to a
	 * <code>maximizeFrame</code> call.
	 * 
	 * @param f
	 *            the <code>JInternalFrame</code> to be restored
	 */
	public void minimizeFrame(JInternalFrame f) {
		super.minimizeFrame(f);
		updateOtherFramesBasedOn(f);
	}

	/**
	 * Removes the frame from its parent and adds its <code>desktopIcon</code>
	 * to the parent.
	 * 
	 * @param f
	 *            the <code>JInternalFrame</code> to be iconified
	 */
	public void iconifyFrame(JInternalFrame f) {
		super.iconifyFrame(f);
		updateOtherFramesBasedOn(null);
	}

	/**
	 * Removes the desktopIcon from its parent and adds its frame to the parent.
	 * 
	 * @param f
	 *            the <code>JInternalFrame</code> to be de-iconified
	 */
	public void deiconifyFrame(JInternalFrame f) {
		super.deiconifyFrame(f);
		updateOtherFramesBasedOn(f);
	}

	/**
	 * This will activate <b>f</b> moving it to the front. It will set the
	 * current active frame's (if any) <code>IS_SELECTED_PROPERTY</code> to
	 * <code>false</code>. There can be only one active frame across all Layers.
	 * 
	 * @param f
	 *            the <code>JInternalFrame</code> to be activated
	 */
	public void activateFrame(JInternalFrame f) {
		super.activateFrame(f);
		updateOtherFramesBasedOn(f);
	}

	// implements javax.swing.DesktopManager
	public void deactivateFrame(JInternalFrame f) {
		super.deactivateFrame(f);
		updateOtherFramesBasedOn(null);
	}

	// implements javax.swing.DesktopManager
	public void endDraggingFrame(JComponent f) {
		super.endDraggingFrame(f);
		updateOtherFramesBasedOn(f);
	}

	// implements javax.swing.DesktopManager
	public void endResizingFrame(JComponent f) {
		super.endResizingFrame(f);
		updateOtherFramesBasedOn(f);
	}

	/** This moves the <code>JComponent</code> and repaints the damaged areas. */
	public void setBoundsForFrame(JComponent f, int newX, int newY,
			int newWidth, int newHeight) {
		f.setBounds(newX, newY, newWidth, newHeight);
		updateOtherFramesBasedOn(f);
		// we must validate the hierarchy to not break the hw/lw mixing
		f.revalidate();
	}
}
