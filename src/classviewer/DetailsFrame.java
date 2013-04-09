package classviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.DefaultCellEditor;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import classviewer.model.CourseModel;
import classviewer.model.CourseModelListener;
import classviewer.model.CourseRec;
import classviewer.model.Status;

/**
 * Show list of offerings, let set offering status, show HTML with course
 * description. Reacts to selections in the course list and calendar.
 * 
 * @author TK
 */
public class DetailsFrame extends NamedInternalFrame implements
		CourseSelectionListener, CourseModelListener {

	private JTable offeringTable;
	private OfferingTableModel tableModel;
	private JTextPane htmlPane;
	private CourseRec selectedCourse = null;

	public DetailsFrame(CourseModel model, Settings settings) {
		super("Details", model);
		this.setLayout(new BorderLayout());
		model.addListener(this);

		Dimension dim = new Dimension(400, 200);
		this.setMinimumSize(dim);
		this.setSize(dim);

		tableModel = new OfferingTableModel(model);
		offeringTable = new JTable(tableModel);
		offeringTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		offeringTable.setDefaultRenderer(Status.class, new StatusCellRenderer(
				settings));
		offeringTable
				.getColumnModel()
				.getColumn(0)
				.setCellEditor(
						new DefaultCellEditor(new StatusComboBox(Status
								.getAll(), settings)));
		this.add(offeringTable, BorderLayout.NORTH);

		htmlPane = new JTextPane();
		this.add(new JScrollPane(htmlPane), BorderLayout.CENTER);
		htmlPane.setContentType("text/html");
		htmlPane.setEditable(false);

		offeringTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						updateHtml();
					}
				});
	}

	@Override
	public void courseSelected(CourseRec course) {
		this.selectedCourse = course;

		tableModel.courseSelected(course);
		setColumnWidth();
		updateHtml();
	}

	private void updateHtml() {
		if (selectedCourse == null) {
			htmlPane.setText("");
		} else {
			String text = "<html>" + selectedCourse.getLongHtml();
			if (offeringTable.getSelectedRow() >= 0) {
				text += "<br/>"
						+ selectedCourse.getOfferings()
								.get(offeringTable.getSelectedRow())
								.getLongHtml();
			}
			htmlPane.setText(text);
		}
	}

	private void setColumnWidth() {
		offeringTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int cw = 20;
		TableColumn column = offeringTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(cw);
		column.setWidth(cw);
		column.setMaxWidth(cw);

		cw = (offeringTable.getWidth() - cw) / 3;
		for (int i = 1; i < 4; i++) {
			column = offeringTable.getColumnModel().getColumn(i);
			column.setPreferredWidth(cw);
			column.setWidth(cw);
			column.setMaxWidth(cw);
		}
	}

	@Override
	public void courseStatusChanged(CourseRec course) {
		if (course == selectedCourse && course != null)
			courseSelected(selectedCourse);
	}

	@Override
	public void modelUpdated() {
		courseSelected(selectedCourse);
	}

	@Override
	public void filtersUpdated() {
		// Noop
	}
}
