package classviewer;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import classviewer.filters.CourseFilter;
import classviewer.model.CourseModel;
import classviewer.model.CourseModelListener;
import classviewer.model.CourseRec;

/**
 * Frame for setting up filters.
 * 
 * Borrowed heavily from
 * http://www.java2s.com/Tutorial/Java/0240__Swing/CheckBoxTreenode.htm
 * 
 * @author TK
 * 
 */
public class CourseFilterFrame extends NamedInternalFrame implements
		CourseModelListener {

	private DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
	private JTree tree;
	private FilterTreeModel treeModel;

	public CourseFilterFrame(CourseModel model) {
		super("Filter", model);

		Dimension dim = new Dimension(50, 50);
		this.setMinimumSize(dim);
		this.setSize(dim);

		model.addListener(this);

		// Make a root node
		initTree();
		treeModel = new FilterTreeModel(root);
		tree = new JTree(treeModel) {
			public boolean isPathEditable(TreePath path) {
				Object comp = path.getLastPathComponent();
				if (getFilterOut(comp) != null)
					return true;
				if (getDescOut(comp) != null)
					return true;
				return false;
			}
		};
		tree.setCellRenderer(new FilterTreeRenderer());
		tree.setCellEditor(new FilterCellEditor());
		tree.setEditable(true);
		tree.setRootVisible(false);
		this.add(new JScrollPane(tree));
	}

	protected void initTree() {
		root.removeAllChildren();
		for (CourseFilter f : courseModel.getFilters()) {
			DefaultMutableTreeNode n = new DefaultMutableTreeNode(f);
			root.add(n);
			for (Object el : f.getOptions())
				n.add(new DefaultMutableTreeNode(el));
		}
	}

	protected static CourseFilter getFilterOut(Object object) {
		if (object instanceof DefaultMutableTreeNode) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
			Object userObject = node.getUserObject();
			if (userObject instanceof CourseFilter)
				return (CourseFilter) userObject;
		}
		return null;
	}

	protected static Object getDescOut(Object object) {
		if (object instanceof DefaultMutableTreeNode) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
			Object userObject = node.getUserObject();
			return userObject;
		}
		return null;
	}

	class FilterTreeRenderer extends DefaultTreeCellRenderer {

		private JCheckBox checkBox = new JCheckBox();

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			CourseFilter filter = getFilterOut(value);
			if (filter != null)
				return prepareCheckbox(filter.getName(), filter.isActive(),
						selected);
			Object option = getDescOut(value);
			if (option != null) {
				filter = getFilterOut(((DefaultMutableTreeNode) value)
						.getParent());
				if (filter != null)
					return prepareCheckbox(filter.getDescription(option),
							filter.isSelected(option), selected);
			}
			return super.getTreeCellRendererComponent(tree, value, selected,
					expanded, leaf, row, hasFocus);
		}

		private JCheckBox prepareCheckbox(String name, boolean state,
				boolean selected) {
			checkBox.setText(name);
			checkBox.setSelected(state);
			if (selected) {
				checkBox.setForeground(getTextSelectionColor());
				checkBox.setBackground(getBackgroundSelectionColor());
			} else {
				checkBox.setForeground(getTextNonSelectionColor());
				checkBox.setBackground(getBackgroundNonSelectionColor());
			}
			return checkBox;
		}

	}

	class FilterTreeModel extends DefaultTreeModel {
		public FilterTreeModel(DefaultMutableTreeNode root) {
			super(root);
		}

		@Override
		public int getChildCount(Object parent) {
			CourseFilter filter = getFilterOut(parent);
			if (filter != null && !filter.isActive())
				return 0;
			return super.getChildCount(parent);
		}

		@Override
		public boolean isLeaf(Object node) {
			return getChildCount(node) == 0;
		}
	}

	class FilterCellEditor extends DefaultCellEditor {

		protected CourseFilter filter;
		protected Object option;
		protected DefaultMutableTreeNode node;

		public FilterCellEditor() {
			super(new JCheckBox());
		}

		public Component getTreeCellEditorComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row) {

			JCheckBox editor = null;
			filter = getFilterOut(value);
			if (filter != null) {
				node = (DefaultMutableTreeNode) value;
				editor = (JCheckBox) (super.getComponent());
				editor.setText(filter.getName());
				editor.setSelected(filter.isActive());
				option = null;
			} else {
				option = getDescOut(value);
				if (option != null) {
					filter = getFilterOut(((DefaultMutableTreeNode) value)
							.getParent());
					if (filter != null) {
						editor = (JCheckBox) (super.getComponent());
						editor.setText(filter.getDescription(option));
						editor.setSelected(filter.isSelected(option));
					} else {
						option = null;
						filter = null;
					}
				}
			}
			return editor;
		}

		public Object getCellEditorValue() {
			JCheckBox editor = (JCheckBox) (super.getComponent());
			if (option != null) {
				filter.setSelected(option, editor.isSelected());
				return option;
			} else {
				filter.setActive(editor.isSelected());
				treeModel.nodeStructureChanged(node);
				// TODO Want to also open the node. The following does it, but
				// prints a stack trace. Need a better approach.
				// if (editor.isSelected()) 
				// tree.expandPath(new TreePath(node.getPath()));
				return filter;
			}
		}
	}

	@Override
	public void courseStatusChanged(CourseRec course) {
		// Noop
	}

	@Override
	public void modelUpdated() {
		initTree();
		treeModel.reload();
	}
}
