package classviewer;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import classviewer.model.Status;

public class StatusCellRenderer extends DefaultTableCellRenderer {

	private Settings settings;

	public StatusCellRenderer(Settings settings) {
		this.settings = settings;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component res = super.getTableCellRendererComponent(table, value,
				isSelected, hasFocus, row, column);
		Status status = (Status) value;
		res.setBackground(settings.getColor(status.toString() + "CalBg"));
		res.setForeground(settings.getColor(status.toString() + "CalFg"));
		return res;
	}

}
