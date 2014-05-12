package classviewer;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import classviewer.model.CourseModel;
import classviewer.model.CourseModelListener;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;
import classviewer.model.Status;

public class OfferingTableModel extends DefaultTableModel implements
		CourseSelectionListener {

	private CourseModel model;
	private CourseRec selected = null;
	private ArrayList<CourseModelListener> listeners = new ArrayList<CourseModelListener>();

	public OfferingTableModel(CourseModel model) {
		this.model = model;
	}
	
	public void addModelReloadListener(CourseModelListener lnr) {
		listeners.add(lnr);
	}

	// public void refreshModel() {
	// courseSelected(null);
	// this.fireTableDataChanged();
	// }

	@Override
	public Class<?> getColumnClass(int col) {
		if (col == 0)
			return Status.class;
		if (col == 2)
			return Integer.class;
		return Object.class;
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0)
			return "?";
		if (col == 1)
			return "Start";
		if (col == 2)
			return "Weeks";
		return "Id";
	}

	@Override
	public int getRowCount() {
		if (selected == null)
			return 0;
		return selected.getOfferings().size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		OffRec off = selected.getOfferings().get(row);
		if (col == 0)
			return off.getStatus();
		if (col == 1)
			return off.asHTML();
		if (col == 2)
			return off.getDuration(); 
		return off.getId();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return col == 0 || col == 2;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		switch (col) {
		case 0:
			selected.getOfferings().get(row).setStatus((Status) value);
			try {
				model.saveStatusFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						"Error saving status file: " + e);
			}
			model.fireCourseStatusChanged(selected);
			break;
		case 2:
			selected.getOfferings().get(row).setDuration((Integer) value);
			try {
				model.saveModelFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Error saving model file: "
						+ e);
			}
			for (CourseModelListener lnr : listeners)
				lnr.modelUpdated();
			break;

		default:
			throw new RuntimeException("Column " + col
					+ " should not be editable");
		}
	}

	@Override
	public void courseSelected(CourseRec course) {
		selected = course;
		this.fireTableDataChanged();
	}
}
