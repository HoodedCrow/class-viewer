package classviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import classviewer.changes.Change;
import classviewer.changes.CourseraModelAdapter;
import classviewer.changes.CourseraModelAdapter1;
import classviewer.changes.EdxModelAdapter;
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

	private ArrayList<Change> changes = null;
	private ArrayList<Boolean> changeSelected = new ArrayList<Boolean>();
	/**
	 * If true, remove changes that modify existing link or professor name to
	 * empty
	 */
	private boolean pruneDropLinkProf = false;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ChangesFrame(CourseModel model, final Settings settings) {
		super("Changes", model);

		Dimension dim = new Dimension(400, 200);
		this.setMinimumSize(dim);
		this.setSize(dim);

		this.settings = settings;
		// model.addListener(this);
		this.setLayout(new BorderLayout());

		JPanel buttons = new JPanel();
		this.add(buttons, BorderLayout.NORTH);

		JButton but = new JButton("Coursera");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadCoursera(new CourseraModelAdapter1(),
						settings.getString(Settings.COURSERA_URL2));
			}
		});
		buttons.add(but);

		but = new JButton("EdX");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadEdx();
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

		final JCheckBox cbox = new JCheckBox("Hide clear link",
				pruneDropLinkProf);
		cbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pruneDropLinkProf = cbox.isSelected();
				List<? extends SortKey> skeys = table.getRowSorter()
						.getSortKeys();
				((ChangeModel) table.getModel()).fireTableChanged(null);
				setColumnWidth();
				table.getRowSorter().setSortKeys(skeys);
			}
		});
		buttons.add(cbox);

		but = new JButton("Accept Active");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				applyActiveChanges();
			}
		});
		buttons.add(but);

		table = new JTable(new ChangeModel());
		table.setAutoCreateRowSorter(true);
		RowFilter<ChangeModel, Object> rf = new RowFilter<ChangeModel, Object>() {
			@Override
			public boolean include(
					javax.swing.RowFilter.Entry<? extends ChangeModel, ? extends Object> entry) {
				if (!pruneDropLinkProf)
					return true;
				Change c = changes.get((Integer) entry.getIdentifier());
				if (!Change.MODIFY.equals(c.getType()))
					return true;
				Object ov = c.getOldValue();
				Object nv = c.getNewValue();
				boolean res = !"".equals(nv) || "".equals(ov);
				return res;
			}
		};
		RowSorter<? extends TableModel> sorter = table.getRowSorter();
		((TableRowSorter) sorter).setRowFilter(rf);
		table.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int col = table.columnAtPoint(e.getPoint());
				if (col != 1) {
					table.setToolTipText(null);
					return;
				}
				int row = table.rowAtPoint(e.getPoint());
				row = table.getRowSorter().convertRowIndexToModel(row);
				Change change = changes.get(row);
				if (change != null)
					table.setToolTipText(change.getToolTip());
				else
					table.setToolTipText(null);
			}
		});
		this.add(new JScrollPane(table), BorderLayout.CENTER);
		setColumnWidth();
	}

	protected void finishModification() {
		try {
			courseModel.saveModelFile();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"Cannot save static data file:\n" + e);
		}
		try {
			courseModel.saveStatusFile();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Cannot save status file:\n"
					+ e);
		}
		List<? extends SortKey> skeys = table.getRowSorter().getSortKeys();
		courseModel.fireModelReloaded();
		table.tableChanged(null);
		setColumnWidth();
		table.getRowSorter().setSortKeys(skeys);
	}

	protected void applySelectedChanges() {
		boolean changed = false;
		for (int i = 0; i < changes.size(); i++)
			if (changeSelected.get(i)) {
				changes.get(i).apply(courseModel);
				changes.remove(i);
				changeSelected.remove(i);
				i--;
				changed = true;
			}
		if (changed)
			finishModification();
	}

	protected void applyActiveChanges() {
		boolean changed = false;
		for (int i = 0; i < changes.size(); i++) {
			if (changes.get(i).isActivation()) {
				changes.get(i).apply(courseModel);
				changes.remove(i);
				changeSelected.remove(i);
				i--;
				changed = true;
			}
		}
		if (changed)
			finishModification();
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

	private class WaitDialog extends JDialog {
		WaitDialog() {
			this.setTitle("Working on it");
			this.setModal(true);
			this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			JProgressBar bar = new JProgressBar();
			bar.setIndeterminate(true);
			this.add(bar);
			this.pack();
			this.setSize(400, this.getHeight());
			this.setLocationRelativeTo(null);
		}
	}

	protected void loadCoursera(final CourseraModelAdapter adapter,
			final String url) {
		final WaitDialog wait = new WaitDialog();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					adapter.load(url);
				} catch (IOException e) {
					wait.setVisible(false);
					JOptionPane.showMessageDialog(null,
							"Failed to load Coursera data:\n" + e);
					return;
				}

				changes = adapter.collectChanges(courseModel);
				Collections.sort(changes);
				changeSelected.clear();
				for (int i = 0; i < changes.size(); i++)
					changeSelected.add(Boolean.FALSE);
				table.tableChanged(null);
				setColumnWidth();
				wait.setVisible(false);
			}
		}).start();
		wait.setVisible(true);
	}

	protected void loadEdx() {
		final WaitDialog wait = new WaitDialog();
		new Thread(new Runnable() {
			@Override
			public void run() {
				EdxModelAdapter edx = new EdxModelAdapter();
				try {
					edx.parse(
							settings.getString(Settings.EDX_URL),
							new Boolean(settings
									.getString(Settings.IGNORE_SSL_CERT)),
							new Boolean(settings
									.getString(Settings.IGNORE_AP_COURSES)));
				} catch (IOException e) {
					wait.setVisible(false);
					JOptionPane.showMessageDialog(null,
							"Failed to load EdX data:\n" + e);
					return;
				}

				changes = edx.collectChanges(courseModel,
						settings.getInt(Settings.OLD_AGE_IN_DAYS, 3650));
				Collections.sort(changes);
				changeSelected.clear();
				for (int i = 0; i < changes.size(); i++)
					changeSelected.add(Boolean.FALSE);
				table.tableChanged(null);
				setColumnWidth();
				wait.setVisible(false);
			}
		}).start();
		wait.setVisible(true);
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
