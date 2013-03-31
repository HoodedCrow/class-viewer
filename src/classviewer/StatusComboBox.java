package classviewer;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import classviewer.model.Status;

/**
 * Combobox for the editor for table column with Status that sticks out outside
 * of the column bounds.
 * 
 * @author TK
 */
public class StatusComboBox extends JComboBox<Status> {
	private Settings settings;

	public StatusComboBox(Status[] statuses, Settings settings) {
		super(statuses);
		this.settings = settings;
		this.setRenderer(new NameCellRenderer());
	}

	/**
	 * The columns are one letter wide, but we want the whole word in the combo
	 * box, so make it stick out, even though it's kinda ugly. Better ideas are
	 * welcome.
	 */
	@Override
	public void setBounds(Rectangle rect) {
		if (rect.width < 70)
			rect.width = 70;
		super.setBounds(rect);
	}

	private class NameCellRenderer implements ListCellRenderer<Status> {
		private DefaultListCellRenderer renderer = new DefaultListCellRenderer();

		@Override
		public Component getListCellRendererComponent(
				JList<? extends Status> list, Status value, int index,
				boolean isSelected, boolean cellHasFocus) {
			Component res = renderer.getListCellRendererComponent(list,
					value.getName(), index, isSelected, cellHasFocus);
			res.setBackground(settings.getColor(value.toString() + "CalBg"));
			res.setForeground(settings.getColor(value.toString() + "CalFg"));
			return res;
		}

	}
}
