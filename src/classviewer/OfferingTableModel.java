package classviewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
	private ArrayList<OffRec> sortedElements = new ArrayList<OffRec>();
	private ArrayList<CourseModelListener> listeners = new ArrayList<CourseModelListener>();
	private final String[] names = { " ", "Start", "Weeks (edit)", "Id" };
	private final Class<?>[] classes = { Status.class, String.class,
			Integer.class, Integer.class };

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
		return classes[col];
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public String getColumnName(int col) {
		return names[col];
	}

	@Override
	public int getRowCount() {
		if (selected == null)
			return 0;
		return sortedElements.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		OffRec off = sortedElements.get(row);
		if (col == 0)
			return off.getStatus();
		if (col == 1)
			return off.asHTML();
		if (col == 2)
			return off.getDuration();
		return off.getId();
	}

	public OffRec getOfferingAt(int row) {
		return sortedElements.get(row);
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return col == 0 || col == 2;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		switch (col) {
		case 0:
			sortedElements.get(row).setStatus((Status) value);
			try {
				model.saveStatusFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						"Error saving status file: " + e);
			}
			model.fireCourseStatusChanged(selected);
			break;
		case 2:
			sortedElements.get(row).setDuration((Integer) value);
			saveModelAndInform();
			break;

		default:
			throw new RuntimeException("Column " + col
					+ " should not be editable");
		}
	}

	/**
	 * Save the model file and inform all listeners. This method is called when
	 * the offering duration is changed by some means
	 */
	protected void saveModelAndInform() {
		try {
			model.saveModelFile();
		} catch (IOException e) {
			JOptionPane
					.showMessageDialog(null, "Error saving model file: " + e);
		}
		for (CourseModelListener lnr : listeners)
			lnr.modelUpdated();
	}

	@Override
	public void courseSelected(CourseRec course) {
		selected = course;
		sortedElements.clear();
		if (selected != null)
			sortedElements.addAll(selected.getOfferings());
		Collections.sort(sortedElements, new Comparator<OffRec>() {
			@Override
			public int compare(OffRec o1, OffRec o2) {
				if (o1.getStart() == null) {
					if (o2.getStart() != null)
						return 1;
					return o1.getId().compareTo(o2.getId());
				} else {
					if (o2.getStart() == null)
						return -1;
					return o1.getStart().compareTo(o2.getStart());
				}
			}
		});
		this.fireTableDataChanged();
	}
}
