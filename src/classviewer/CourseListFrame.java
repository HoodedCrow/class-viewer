package classviewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JScrollPane;
import javax.swing.JTable;
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
	private ArrayList<CourseRec> cachedCourses = new ArrayList<CourseRec>();

	private static final int SORT_BY_NAME = 1;
	private static final int SORT_BY_STATUS = 2;

	private int sortMode = SORT_BY_NAME;

	public CourseListFrame(CourseModel model) {
		super("Courses", model);
		model.addListener(this);

		table = new JTable(new CourseTableModel());
		this.add(new JScrollPane(table));

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
		cachedCourses.clear();
		cachedCourses.addAll(courseModel.getFilteredCourses());

		// Sort
		if (sortMode == SORT_BY_NAME) {
			Collections.sort(cachedCourses, new Comparator<CourseRec>() {
				@Override
				public int compare(CourseRec o1, CourseRec o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
		// TODO Sort by status

		((CourseTableModel) table.getModel()).fireTableChanged(null);
		setColumnWidth();
	}

	private class CourseTableModel extends DefaultTableModel {

		@Override
		public int getRowCount() {
			return cachedCourses.size();
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
			CourseRec rec = cachedCourses.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return rec.getStatus();
			case 1: 
				return (rec.hasDateless()?"?":"") + (rec.hasUnknown()?"+":"");
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
