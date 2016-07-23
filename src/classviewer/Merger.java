package classviewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;
import classviewer.model.Source;
import classviewer.model.StatusFileModelAdapter;
import classviewer.model.XmlModelAdapter;

public class Merger extends JFrame {
	private static SimpleDateFormat dformat = new SimpleDateFormat("MM/dd/yy");

	private Settings settings;
	private CourseModel model;
	private ArrayList<CourseRec> courses;
	private int currentIndex;
	private JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	private JTextPane left, right;
	private JButton takeLeft, takeRight, skipIt, save;
	static Source source = Source.COURSERA;

	private Merger(Settings settings) {
		super("Course record merger");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.settings = settings;
		this.model = new CourseModel(settings);

		JPanel buttons = new JPanel();
		this.setLayout(new BorderLayout());
		// JScrollPane scroll = new JScrollPane(split);
		this.add(split, BorderLayout.CENTER);
		this.add(buttons, BorderLayout.SOUTH);
		left = new JTextPane();
		left.setContentType("text/html");
		right = new JTextPane();
		right.setContentType("text/html");
		split.setLeftComponent(new JScrollPane(left));
		split.setRightComponent(new JScrollPane(right));

		takeLeft = new JButton("Left is new");
		takeLeft.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				merge(courses.get(currentIndex), courses.get(currentIndex + 1));
			}
		});
		takeRight = new JButton("Right is new");
		takeRight.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				merge(courses.get(currentIndex + 1), courses.get(currentIndex));
			}
		});
		skipIt = new JButton("Skip");
		skipIt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				currentIndex++;
				findNextPair();
			}
		});
		save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveModel();
			}
		});
		buttons.add(takeLeft);
		buttons.add(skipIt);
		buttons.add(takeRight);
		buttons.add(save);

		left.setText("Left");
		right.setText("Right");
		this.pack();
		this.setSize(1000, 800);
	}

	private void findNextPair() {
		while (currentIndex < courses.size() - 1) {
			CourseRec one = courses.get(currentIndex);
			CourseRec two = courses.get(currentIndex + 1);
			if (!one.getName().equals(two.getName())) {
				currentIndex++;
				continue;
			}
			left.setText(summarize(one));
			right.setText(summarize(two));
			split.setDividerLocation(0.5);
			return;
		}
		left.setText("The end");
		right.setText("");
		takeLeft.setEnabled(false);
		takeRight.setEnabled(false);
		skipIt.setEnabled(false);
	}

	private String summarize(CourseRec course) {
		StringBuffer buf = new StringBuffer();
		buf.append("<b>" + course.getName() + "</b><br/>");
		buf.append(course.getId() + " <b>" + course.getStatus() + "</b><br/>");
		buf.append(course.getCategories() + "<br/>");
		buf.append(course.getUniversities() + "<br/><br/>");
		ArrayList<OffRec> offs = new ArrayList<OffRec>(course.getOfferings());
		Collections.sort(offs, new Comparator<OffRec>() {
			@Override
			public int compare(OffRec o1, OffRec o2) {
				if (o1.getStart() == null) {
					if (o2.getStart() == null) return 0;
					return 1;
				}
				if (o2.getStart() == null)
					return -1;
				return o1.getStart().compareTo(o2.getStart());
			}
		});
		for (OffRec off : offs) {
			if (off.getStart() == null) {
				buf.append("no date ");
			} else {
				buf.append(dformat.format(off.getStart()) + " ");
			}
			buf.append("<b>" + off.getStatus() + "</b> ");
			buf.append(off.getId() + "<br/>");
		}
		return buf.toString();
	}

	private void merge(CourseRec newRec, CourseRec oldRec) {
		for (OffRec or : oldRec.getOfferings()) {
			newRec.addOffering(or);
		}
		oldRec.getOfferings().clear();
		newRec.setStatus(oldRec.getStatus());
		model.removeCourse(source, oldRec.getId());

		left.setText(summarize(newRec));
		right.setText("");
	}

	private void saveModel() {
		try {
			model.saveModelFile();
			model.saveStatusFile();
			System.out.println("Saved");
		} catch (IOException e) {
			JOptionPane
					.showMessageDialog(this, "Cannot save data files:\n" + e);
		}
	}

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
		}

		courses = new ArrayList<CourseRec>(model.getCourses(source));
		Collections.sort(courses, new Comparator<CourseRec>() {
			@Override
			public int compare(CourseRec o1, CourseRec o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		currentIndex = 0;
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		String settingsFileName = null;
		for (int i = 0; i < args.length; i++) {
			if ("-i".equals(args[i])) {
				settingsFileName = args[++i];
			} else {
				System.err.println("Ignoring parameter " + args[i]);
			}
		}
		Merger me = new Merger(new Settings(settingsFileName));
		me.load();
		me.findNextPair();
		me.setVisible(true);
	}
}
