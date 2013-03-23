package classviewer;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

/**
 * "Desktop" view with internal frames. This class also keeps references to the
 * models.
 * 
 * @author TK
 */
public class CourseViewer extends JFrame {

	private CourseFilterFrame courseFilterFrame;

	private CourseViewer() {
		super("Course Viewer/Chooser");

		JDesktopPane desktop = new JDesktopPane();
		this.setContentPane(desktop);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

		this.setSize(800, 600);

		JMenuBar topMenu = new JMenuBar();
		this.setJMenuBar(topMenu);
		JMenu windowMenu = new JMenu("Window");
		topMenu.add(windowMenu);

		courseFilterFrame = new CourseFilterFrame();
		windowMenu.add(courseFilterFrame.makeCheckBoxMenuItem());
		desktop.add(courseFilterFrame);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		CourseViewer me = new CourseViewer();
		me.setVisible(true);
	}

}
