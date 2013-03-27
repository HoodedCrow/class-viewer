package classviewer;

import java.util.ArrayList;

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

/**
 * Filtered list of courses.
 * 
 * @author TK
 */
public class CourseListFrame extends NamedInternalFrame implements
		CourseModelListener {

	private final String[] columnNames = { "", "", "Name" };
	private JTable table;
	private ArrayList<CourseSelectionListener> courseListeners = new ArrayList<CourseSelectionListener>();

	public CourseListFrame(CourseModel model) {
		super("Courses", model);
		model.addListener(this);

		final CourseTableModel tableModel = new CourseTableModel();
		table = new JTable(tableModel);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
			// TODO Auto-generated method stub
			return Object.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return false;
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
			// TODO Auto-generated method stub

		}
	}
}
