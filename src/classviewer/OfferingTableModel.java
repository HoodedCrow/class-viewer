package classviewer;

import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import classviewer.model.CourseModel;
import classviewer.model.CourseModelListener;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public class OfferingTableModel extends DefaultTableModel implements
		CourseSelectionListener {

//	private CourseModel model;

	private CourseRec selected = null;

//	public OfferingTableModel(CourseModel model) {
//		this.model = model;
//	}

//	public void refreshModel() {
//		courseSelected(null);
//		this.fireTableDataChanged();
//	}

	@Override
	public Class<?> getColumnClass(int col) {
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
			return "Duration";
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
			return off.getDurStr(); // getHome();
		return off.getId();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false; // TODO col == 0;
	}
/*
	@Override
	public void setValueAt(Object value, int row, int col) {
		assert (col == 0);
		int idx = ((StatusEnum) value).ordinal() + 1;
		selected.getOfferings().get(row).setStatus(idx);
		try {
			model.saveStatusFile();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Error saving status file: "
					+ e);
		}
		model.notifyListeners(selected);
	}
*/
	@Override
	public void courseSelected(CourseRec course) {
		selected = course;
		this.fireTableDataChanged();
	}
/*
	@Override
	public void courseStatusChanged(CourseRec course) {
		fireTableDataChanged();
	}
	*/
}
