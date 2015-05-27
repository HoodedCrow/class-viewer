package classviewer.changes;

import java.util.Collection;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.DescRec;
import classviewer.model.OffRec;

/**
 * Add/delete course or change course field.
 * 
 * @author TK
 */
public final class CourseChange {
	private CourseChange() {
	}

	public static Change add(final CourseRec record,
			final Collection<String> categories,
			final Collection<String> universities) {
		Change change = new Change(record.getSource(), Change.ADD) {
			@Override
			public String getDescription() {
				return (record.isSelfStudy() ? "#" : "") + "Class";
			}

			@Override
			public String getTarget() {
				return record.getName();
			}

			@Override
			public String getNewValue() {
				return record.getId();
			}

			@Override
			public String getOldValue() {
				return null;
			}

			@Override
			public void apply(CourseModel model) {
				for (String s : categories)
					record.addCategory(model.getCategory(record.getSource(), s));
				for (String s : universities)
					record.addUniversity(model.getUniversity(
							record.getSource(), s));
				model.addCourse(record);
			}
		};
		StringBuffer b = new StringBuffer("<html><b>New class: ");
		b.append(record.getName()).append("<br/>");
		if (!universities.isEmpty())
			b.append("From ").append(universities);
		if (record.getInstructor() != null && !record.getInstructor().isEmpty())
			b.append(" by ").append(record.getInstructor());
		if (!categories.isEmpty())
			b.append(" ").append(categories);
		b.append("</b><br/>");
		boolean first = true;
		if (!record.getOfferings().isEmpty()) {
			b.append("Sessions:");
			for (OffRec or : record.getOfferings())
				if (or.getStart() != null) {
					if (!first)
						b.append(",");
					else
						first = false;
					b.append(" ").append(OffRec.dformat.format(or.getStart()));
				}
			b.append("<br/>");
		}
		b.append("<div width=500px>").append(record.getDescription()).append("</div>");
		return change.setOrder(2).setToolTop(b.toString());
	}

	public static Change delete(final CourseRec record) {
		Change change = new Change(record.getSource(), Change.DELETE) {
			@Override
			public String getDescription() {
				return (record.isSelfStudy() ? "#" : "") + "Class";
			}

			@Override
			public String getTarget() {
				return "[" + record.getStatus() + "]" + record.getName();
			}

			@Override
			public String getNewValue() {
				return null;
			}

			@Override
			public String getOldValue() {
				return record.getId();
			}

			@Override
			public void apply(CourseModel model) {
				model.removeCourse(record.getSource(), record.getId());
			}
		};
		StringBuffer b = new StringBuffer("<html><b>Drop class: ");
		b.append(record.getName()).append("[").append(record.getStatus()).append("]<br/>");
		b.append("From ").append(CourseRec.idSet(record.getUniversities()));
		if (record.getInstructor() != null && !record.getInstructor().isEmpty())
			b.append(" by ").append(record.getInstructor());
		b.append("</b><br/>");
		boolean first = true;
		if (!record.getOfferings().isEmpty()) {
			b.append("Sessions:");
			for (OffRec or : record.getOfferings()) {
				if (!first) 
					b.append(",");
				else
					first = false;
				if (or.getStart() != null)
					b.append(" ").append(OffRec.dformat.format(or.getStart()));
			}
			b.append("<br/>");
		}
		b.append("<div width=500px>").append(record.getDescription()).append("</div>");
		return change.setOrder(6).setToolTop(b.toString());
	}

	private static abstract class StringCourseChange extends
			FieldChange<String> {
		private String id;

		StringCourseChange(CourseRec record, String field, String newValue,
				String oldValue) {
			super(record.getSource(), field, newValue, oldValue);
			this.id = "[" + record.getStatus() + "]" + record.getName();
			StringBuffer b = new StringBuffer("<html><b>Change ");
			b.append(field).append(" for ").append(record.getName())
					.append("[").append(record.getStatus()).append("]<br/>");
			b.append("New value: <div width=400px>").append(newValue);
			b.append("</div><br/>Old value: <div width=400px>")
					.append(oldValue).append("</div>");
			setOrder(4);
			setToolTop(b.toString());
		}

		@Override
		public Object getTarget() {
			return id;
		}
	}

	public static Change setName(final CourseRec record, final String newValue) {
		return new StringCourseChange(record, "Name", newValue,
				record.getName()) {
			@Override
			public void apply(CourseModel model) {
				record.setName(newValue);
			}
		};
	}

	public static Change setDescription(final CourseRec record,
			final String newValue) {
		return new StringCourseChange(record, "Description", newValue,
				record.getDescription()) {
			@Override
			public void apply(CourseModel model) {
				record.setDescription(newValue);
			}
		};
	}

	public static Change setShortName(final CourseRec record,
			final String newValue) {
		return new StringCourseChange(record, "Short name", newValue,
				record.getShortName()) {
			@Override
			public void apply(CourseModel model) {
				record.setShortName(newValue);
			}
		};
	}

	public static Change setInstructor(final CourseRec record,
			final String newValue) {
		return new StringCourseChange(record, "Instructor", newValue,
				record.getInstructor()) {
			@Override
			public void apply(CourseModel model) {
				record.setInstructor(newValue);
			}
		};
	}

	public static Change setLink(final CourseRec record, final String newValue) {
		return new StringCourseChange(record, "Link", newValue,
				record.getLink()) {
			@Override
			public void apply(CourseModel model) {
				record.setLink(newValue);
			}
		};
	}

	public static Change setLanguage(final CourseRec record,
			final String newValue) {
		return new StringCourseChange(record, "Language", newValue,
				record.getLanguage()) {
			@Override
			public void apply(CourseModel model) {
				record.setLanguage(newValue);
			}
		};
	}

	private static abstract class IdSetCourseChange extends
			FieldChange<Collection<String>> {
		private String id;

		IdSetCourseChange(CourseRec record, String field,
				Collection<String> newValue, Collection<String> oldValue) {
			super(record.getSource(), field, newValue, oldValue);
			this.id = "[" + record.getStatus() + "]" + record.getName();
			setOrder(4);
		}

		@Override
		public Object getTarget() {
			return id;
		}
	}

	public static Change setCategories(final CourseRec record,
			final Collection<String> newValue) {
		return new IdSetCourseChange(record, "Categories", newValue,
				CourseRec.idSet(record.getCategories())) {
			@Override
			public void apply(CourseModel model) {
				record.getCategories().clear();
				for (String s : newValue) {
					DescRec cat = model.getCategory(record.getSource(), s);
					if (cat == null)
						System.err.println("Cannot find category for id " + s);
					else
						record.addCategory(cat);
				}
			}
		};
	}

	public static Change setUniversities(final CourseRec record,
			final Collection<String> newValue) {
		return new IdSetCourseChange(record, "Universities", newValue,
				CourseRec.idSet(record.getUniversities())) {
			@Override
			public void apply(CourseModel model) {
				record.getUniversities().clear();
				for (String s : newValue) {
					DescRec uni = model.getUniversity(record.getSource(), s);
					if (uni == null)
						System.err
								.println("Cannot find university for id " + s);
					else
						record.addUniversity(uni);
				}
			}
		};
	}
}
