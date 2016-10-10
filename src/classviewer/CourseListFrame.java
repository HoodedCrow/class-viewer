package classviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import classviewer.model.CourseModel;
import classviewer.model.CourseModelListener;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;
import classviewer.model.Status;

/**
 * Filtered list of courses.
 * 
 * @author TK
 */
public class CourseListFrame extends NamedInternalFrame implements
		CourseModelListener, GraphicSelectionListener, ChangeSelectionListener {

	private final String[] columnNames = { "", "", "Name" };
	private JTable table;
	private ArrayList<CourseSelectionListener> courseListeners = new ArrayList<CourseSelectionListener>();
	private DefaultCellEditor cellEditor;
	private JTextField searchField = new JTextField();
	private int searchFromIdx = 0;

	public CourseListFrame(CourseModel model, Settings settings) {
		super("Courses", model);
		model.addListener(this);

		Dimension dim = new Dimension(400, 200);
		this.setMinimumSize(dim);
		this.setSize(dim);

		final CourseTableModel tableModel = new CourseTableModel();
		table = new JTable(tableModel);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setDefaultRenderer(Status.class, new StatusCellRenderer(settings));
		this.cellEditor = new DefaultCellEditor(new StatusComboBox(
				Status.getAll(), settings));
		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(table), BorderLayout.CENTER);
		this.add(searchField, BorderLayout.SOUTH);

		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting())
							return;
						CourseRec selection = null;
						if (table.getSelectedRow() >= 0)
							// Assume column -1 return the whole object
							selection = (CourseRec) tableModel.getValueAt(table
									.convertRowIndexToModel(table
											.getSelectedRow()), -1);
						for (CourseSelectionListener lnr : courseListeners)
							lnr.courseSelected(selection);
					}
				});
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				searchByName(searchField.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				searchByName(searchField.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// Paranoia.
				throw new RuntimeException(
						"Plain text components do not fire these events");
			}
		});
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int c = e.getKeyCode();
				if (c == KeyEvent.VK_ESCAPE) {
					e.consume();
					searchField.setText("");
				}
				if (c == KeyEvent.VK_DOWN) {
					e.consume();
					searchFromIdx++;
					searchByName(searchField.getText());
				}
			}
		});
		setColumnWidth();
	}

	private void setColumnWidth() {
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		int cw = 20;
		TableColumn column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(cw);
		column.setWidth(cw);
		column.setMaxWidth(cw);
		cw = 30;
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(cw);
		column.setWidth(cw);
		column.setMaxWidth(cw);
		table.getColumnModel().getColumn(0).setCellEditor(cellEditor);
	}
	
	protected void searchByName(String mask) {
		if (mask.isEmpty()) {
			searchFromIdx = 0;
			return;
		}
		try {
			Pattern p = Pattern.compile(mask);
			ArrayList<CourseRec> all = courseModel.getFilteredCourses();
			for (int i = searchFromIdx; i < all.size(); i++) {
				CourseRec cr = all.get(i);
				if (p.matcher(cr.getName()).find()) {
					selectCourse(cr);
					searchFromIdx = i;
					return;
				}
			}
			// If not found, restart from the top. This goes recursively once.
			if (searchFromIdx > 0) {
				searchFromIdx = 0;
				searchByName(mask);
			}
		} catch (Exception e) {
		}
	}
	
	protected void selectCourse(CourseRec course) {
		int idx = courseModel.getFilteredCourses().indexOf(course);
		idx = table.convertRowIndexToView(idx);
		table.getSelectionModel().setSelectionInterval(idx, idx);
		table.scrollRectToVisible(table.getCellRect(idx, 0, true));
	}

	@Override
	public void offeringClicked(OffRec offering, boolean toKill) {
		CourseRec course = offering.getCourse();
		selectCourse(course);
		
		if (toKill && offering.getStatus() != Status.NO) {
			offering.setStatus(Status.NO);
			try {
				courseModel.saveStatusFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						"Error saving status file: " + e);
			}
			courseModel.fireCourseStatusChanged(offering.getCourse());
		}
	}

	@Override
	public void offeringChangeSelected(OffRec offering) {
		selectCourse(offering.getCourse());
		// TODO Also select the offering in the details panel.
	}

	@Override
	public void courseChangeSelected(CourseRec course) {
		selectCourse(course);
	}

	@Override
	public void courseStatusChanged(CourseRec course) {
		// TODO Auto-generated method stub

	}

	@Override
	public void modelUpdated() {
		reloadModel();
	}

	@Override
	public void filtersUpdated() {
		reloadModel();
	}

	private void reloadModel() {
		List<? extends SortKey> sortKeys = table.getRowSorter().getSortKeys();
		((CourseTableModel) table.getModel()).fireTableChanged(null);
		setColumnWidth();
		table.getRowSorter().setSortKeys(sortKeys);
		setTitle("Courses (" + table.getRowCount() + ")");
	}

	public void addSelectionListener(CourseSelectionListener listener) {
		this.courseListeners.add(listener);
	}

	private class CourseTableModel extends DefaultTableModel {

		@Override
		public int getRowCount() {
			return courseModel.getFilteredCourses().size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0)
				return Status.class;
			return Object.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			CourseRec rec = courseModel.getFilteredCourses().get(rowIndex);
			switch (columnIndex) {
			// This is used to get the whole object
			case -1:
				return rec;
			case 0:
				return rec.getStatus();
			case 1:
				return (rec.hasDateless() ? "?" : "")
						+ (rec.hasUnknown() ? "+" : "");
			case 2:
				return rec.getName();
			}
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			assert (columnIndex == 0);
			CourseRec rec = courseModel.getFilteredCourses().get(rowIndex);
			rec.setStatus((Status) aValue);
			try {
				courseModel.saveStatusFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						"Error saving status file: " + e);
			}
			courseModel.fireCourseStatusChanged(rec);
		}
	}
}
