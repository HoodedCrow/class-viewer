package classviewer.changes;

import classviewer.model.CourseModel;
import classviewer.model.DescRec;
import classviewer.model.Source;

/**
 * Add/delete/modify university or category.
 * 
 * @author TK
 */
public final class DescChange {
	public static final int UNIVERSITY = 1;
	public static final int CATEGORY = 2;

	private DescChange() {
	}

	public static Change add(final int what, Source source, final String id,
			final String name, final String description) {
		final DescRec record = new DescRec(source, id, name, description);
		Change change = new Change(source, Change.ADD, record) {
			@Override
			public String getDescription() {
				return what == UNIVERSITY ? "University" : "Category";
			}

			@Override
			public String getTarget() {
				return id;
			}

			@Override
			public String getNewValue() {
				return name;
			}

			@Override
			public String getOldValue() {
				return null;
			}

			@Override
			public void apply(CourseModel model) {
				if (what == UNIVERSITY)
					model.addUniversity(record);
				else
					model.addCategory(record);
			}
		};
		return change.setOrder(1);
	}

	public static Change delete(final int what, final DescRec record) {
		Change change = new Change(record.getSource(), Change.DELETE, record) {
			@Override
			public String getDescription() {
				return what == UNIVERSITY ? "University" : "Category";
			}

			@Override
			public String getTarget() {
				return record.getId();
			}

			@Override
			public String getNewValue() {
				return null;
			}

			@Override
			public String getOldValue() {
				return record.getName();
			}

			@Override
			public void apply(CourseModel model) {
				if (what == UNIVERSITY)
					model.removeUniversity(record.getSource(), record.getId());
				else
					model.removeCategory(record.getSource(), record.getId());
			}
		};
		return change.setOrder(7);
	}

	public static Change setName(final DescRec record, final String newName) {
		Change change = new FieldChange<String>(record.getSource(), "Name",
				newName, record.getName(), record) {
			@Override
			public Object getTarget() {
				return record.getId();
			}

			@Override
			public void apply(CourseModel model) {
				record.setName(newName);
			}
		};
		return change.setOrder(4);
	}

	public static Change setDescription(final DescRec record,
			final String newDescription) {
		Change change = new FieldChange<String>(record.getSource(),
				"Description", newDescription, record.getDescription(), record) {
			@Override
			public Object getTarget() {
				return record.getId();
			}

			@Override
			public void apply(CourseModel model) {
				record.setDescription(newDescription);
			}
		};
		return change.setOrder(4);
	}
}
