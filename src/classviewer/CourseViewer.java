package classviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import classviewer.model.CourseModel;
import classviewer.model.XmlModelAdapter;

/**
 * "Desktop" view with internal frames. This class also keeps references to the
 * models.
 * 
 * @author TK
 */
public class CourseViewer extends JFrame {

	private Settings settings;
	private CourseModel model;
	private CourseFilterFrame courseFilterFrame;

	private CourseViewer(Settings settings) {
		super("Course Viewer/Chooser");
		this.settings = settings;

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

	/** Load settings and existing data */
	private void load() {
		// Load main model, if present
		model = new CourseModel();
		File file = settings.getStaticFile();
		try {
			FileInputStream reader = new FileInputStream(file);
			XmlModelAdapter xml = new XmlModelAdapter();
			xml.readModel(reader, model);
			reader.close();
			
			
			xml.writeModel(System.out, model); // TODO Kill once works
		} catch (FileNotFoundException e) {
			System.err.println("Initial data file " + file
					+ " is not found. Starting empty.");
			// TODO Check/clean up status data (?)
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Load statuses
		// TODO somewhere load JSON
	}

	private static void printUsageInfo() {
		System.out.println("CourseViewer\n\t-h\tShow this message\n"
				+ "\t-i file.ini\tSpecify settings file. Default: "
				+ Settings.DEFAULT_SETTINGS_FILENAME + "\n");
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		String settingsFileName = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-h")) {
				printUsageInfo();
				return;
			} else if ("-i".equals(args[i])) {
				settingsFileName = args[++i];
			} else {
				System.err.println("Ignoring parameter " + args[i]);
			}
		}
		CourseViewer me = new CourseViewer(new Settings(settingsFileName));
		me.load();
		me.setVisible(true);
	}
}
