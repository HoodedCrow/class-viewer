package classviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import classviewer.changes.Change;
import classviewer.changes.JsonModelAdapter;
import classviewer.model.CourseModel;

/**
 * Frame for loading changes (only Coursera so far) and applying them to the
 * model. URL to load from is in the settings.
 * 
 * @author TK
 */
public class ChangesFrame extends NamedInternalFrame {

	private JTable table;
	private Settings settings;

	/** Debug JSON data? Should be settings configurable */
	private boolean debugJson = false;
	private ArrayList<Change> changes = null;
	private ArrayList<Boolean> changeSelected = new ArrayList<Boolean>();

	public ChangesFrame(CourseModel model, Settings settings) {
		super("Changes", model);

		Dimension dim = new Dimension(400, 200);
		this.setMinimumSize(dim);
		this.setSize(dim);

		this.settings = settings;
		// model.addListener(this);
		this.setLayout(new BorderLayout());

		JPanel buttons = new JPanel();
		this.add(buttons, BorderLayout.NORTH);

		JButton but = new JButton("Load Coursera");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadCoursera();
			}
		});
		buttons.add(but);
		but = new JButton("Select all");
		but.addActionListener(new SelectListener(true));
		buttons.add(but);
		but = new JButton("Deselect all");
		but.addActionListener(new SelectListener(false));
		buttons.add(but);
		but = new JButton("Apply selected");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				applySelectedChanges();
			}
		});
		buttons.add(but);

		table = new JTable(new ChangeModel());
		table.setAutoCreateRowSorter(true);
		this.add(new JScrollPane(table), BorderLayout.CENTER);
		setColumnWidth();
	}

	protected void applySelectedChanges() {
		for (int i = 0; i < changes.size(); i++)
			if (changeSelected.get(i)) {
				changes.get(i).apply(courseModel);
				changes.remove(i);
				changeSelected.remove(i);
				i--;
			}
		try {
			courseModel.saveModelFile();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"Cannot save static data file:\n" + e);
		}
		courseModel.fireModelReloaded();
		table.tableChanged(null);
		setColumnWidth();
	}

	/** Action listener for select all / deselect all buttons */
	private class SelectListener implements ActionListener {
		private Boolean value;

		public SelectListener(boolean value) {
			this.value = value;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			for (int i = 0; i < changeSelected.size(); i++)
				changeSelected.set(i, value);
			table.repaint();
		}
	}

	private void setColumnWidth() {
		// table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		final int[] widths = { 20, 20, 100, 300, 400, 400 };
		for (int i = 0; i < widths.length; i++) {
			TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(widths[i]);
			column.setWidth(widths[i]);
			column.setMaxWidth(widths[i]);
		}
		table.getColumnModel().getColumn(1)
				.setCellRenderer(new OperationCellRenderer());
	}

	protected void loadCoursera() {
		JsonModelAdapter json = new JsonModelAdapter();
		try {
			json.load(settings.getString(Settings.COURSERA_URL));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"Failed to load Coursera data:\n" + e);
			return;
		}

		if (debugJson) {
			System.out.println("Course-level keys");
			System.out.println(json.getCourseLevelKeys());
			System.out.println("Offering-level keys");
			System.out.println(json.getOfferingLevelKeys());
		}

		changes = json.collectChanges(courseModel);
		Collections.sort(changes, new Comparator<Change>() {
			@Override
			public int compare(Change o1, Change o2) {
				// Add category/uni first  
				// Add class
				// Add offering
				// Change anything
				// Delete offering
				// Delete class
				// Delete category/uni
				return Integer.compare(o1.getOrder(), o2.getOrder());
			}
		});
		changeSelected.clear();
		for (int i = 0; i < changes.size(); i++)
			changeSelected.add(Boolean.FALSE);
		table.tableChanged(null);
		setColumnWidth();
	}

	private class ChangeModel extends DefaultTableModel {

		@Override
		public int getRowCount() {
			if (changes == null)
				return 0;
			return changes.size();
		}

		@Override
		public int getColumnCount() {
			return 6;
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
			case 3:
				return "Target";
			case 4:
				return "New value";
			case 5:
				return "Old value";
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0)
				return Boolean.class;
			return Object.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Change ch = changes.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return changeSelected.get(rowIndex);
			case 1:
				return ch.getType();
			case 2:
				return ch.getDescription();
			case 3:
				return ch.getTarget();
			case 4:
				return ch.getNewValue();
			case 5:
				return ch.getOldValue();
			}
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			assert (columnIndex == 0);
			changeSelected.set(rowIndex, (Boolean) aValue);
		}
	}

	private class OperationCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Component res = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
			if (Change.ADD.equals(value))
				res.setBackground(Color.GREEN);
			else if (Change.DELETE.equals(value))
				res.setBackground(Color.RED);
			else
				res.setBackground(table.getBackground());
			res.setForeground(Color.BLACK);
			return res;
		}
	}
}
