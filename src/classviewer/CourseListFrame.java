package classviewer;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
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
		CourseModelListener, GraphicSelectionListener {

	private final String[] columnNames = { "", "", "Name" };
	private JTable table;
	private ArrayList<CourseSelectionListener> courseListeners = new ArrayList<CourseSelectionListener>();
	private DefaultCellEditor cellEditor;

	public CourseListFrame(CourseModel model, Settings settings) {
		super("Courses", model);
		model.addListener(this);

		final CourseTableModel tableModel = new CourseTableModel();
		table = new JTable(tableModel);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setDefaultRenderer(Status.class, new StatusCellRenderer(settings));
		this.cellEditor = new DefaultCellEditor(new StatusComboBox(
				Status.getAll(), settings));
		this.add(new JScrollPane(table));

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

	@Override
	public void offeringClicked(OffRec offering) {
		CourseRec course = offering.getCourse();
		int idx = courseModel.getFilteredCourses().indexOf(course);
		idx = table.convertRowIndexToView(idx);
		table.getSelectionModel().setSelectionInterval(idx, idx);
		table.scrollRectToVisible(table.getCellRect(idx, 0, true));
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
		((CourseTableModel) table.getModel()).fireTableChanged(null);
		setColumnWidth();
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
