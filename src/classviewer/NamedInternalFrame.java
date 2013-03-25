package classviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import classviewer.model.CourseModel;

/**
 * A derivation of the basic internal frame with hooks for frame name and
 * manipulation.
 * 
 * @author TK
 */
public class NamedInternalFrame extends JInternalFrame {
	protected CourseModel courseModel;

	public NamedInternalFrame(String name, CourseModel model) {
		super(name, false, // resizable
				true, // closable
				false, // maximizable
				false);// iconifiable);
		this.setName(name);
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setVisible(true);
		this.courseModel = model;
	}

	/**
	 * Create a brand new check box menu item to show/hide this frame. Set
	 * listeners going both ways.
	 */
	public JCheckBoxMenuItem makeCheckBoxMenuItem() {
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(this.getName());
		item.setSelected(this.isVisible());
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				NamedInternalFrame.this.setVisible(item.isSelected());
			}
		});
		this.addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosing(InternalFrameEvent e) {
				item.setSelected(false);
			}
		});
		return item;
	}
}
