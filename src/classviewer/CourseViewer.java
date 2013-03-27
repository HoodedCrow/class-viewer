package classviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import classviewer.model.CourseModel;
import classviewer.model.StatusFileModelAdapter;
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
	private CourseListFrame courseListFrame;
	private CalendarFrame calendarFrame;
	private DetailsFrame detailsFrame;
	private ChangesFrame changesFrame;

	private CourseViewer(Settings settings) {
		super("Course Viewer/Chooser");
		this.settings = settings;

		JDesktopPane desktop = new JDesktopPane();
		this.setContentPane(desktop);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

		JMenuBar topMenu = new JMenuBar();
		this.setJMenuBar(topMenu);
		JMenu windowMenu = new JMenu("Window");
		topMenu.add(windowMenu);

		model = new CourseModel();
		courseFilterFrame = new CourseFilterFrame(model);
		windowMenu.add(courseFilterFrame.makeCheckBoxMenuItem());
		desktop.add(courseFilterFrame);

		courseListFrame = new CourseListFrame(model);
		windowMenu.add(courseListFrame.makeCheckBoxMenuItem());
		desktop.add(courseListFrame);

		calendarFrame = new CalendarFrame(model, settings);
		windowMenu.add(calendarFrame.makeCheckBoxMenuItem());
		desktop.add(calendarFrame);

		detailsFrame = new DetailsFrame(model);
		windowMenu.add(detailsFrame.makeCheckBoxMenuItem());
		desktop.add(detailsFrame);

		changesFrame = new ChangesFrame(model);
		windowMenu.add(changesFrame.makeCheckBoxMenuItem());
		desktop.add(changesFrame);

		courseListFrame.addSelectionListener(detailsFrame);

		desktop.setLayout(new MainWindowLayout(courseFilterFrame,
				courseListFrame, calendarFrame, detailsFrame, changesFrame));

		this.setSize(800, 600); // so it's not a dot when un-maximized
		this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
	}

	/** Load settings and existing data */
	private void load() {
		// Load main model, if file is present
		boolean haveOldData = false;
		File file = settings.getStaticFile();
		try {
			FileInputStream reader = new FileInputStream(file);
			XmlModelAdapter xml = new XmlModelAdapter();
			xml.readModel(reader, model);
			reader.close();
			haveOldData = true;
			// xml.writeModel(System.out, model); // TODO Kill once works
		} catch (FileNotFoundException e) {
			System.err.println("Initial data file " + file
					+ " is not found. Starting empty.");
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (haveOldData) {
			file = settings.getStatusFile();
			try {
				FileReader reader = new FileReader(file);
				StatusFileModelAdapter adap = new StatusFileModelAdapter();
				adap.updateStatuses(reader, model);
				reader.close();
			} catch (FileNotFoundException e) {
				System.err.println("Status file " + file
						+ " is not found. Assuming UNKNOWN for everything.");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			model.fireModelReloaded();
		}

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
