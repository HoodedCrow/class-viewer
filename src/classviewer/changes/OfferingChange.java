package classviewer.changes;

import java.util.Date;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;

public final class OfferingChange {
	public static Change add(final CourseRec course, final OffRec offering) {
		Change change = new Change(course.getSource(), Change.ADD) {
			@Override
			public String getDescription() {
				return "Offering";
			}

			@Override
			public Object getTarget() {
				return "[" + course.getStatus() + "]" + course.getName();
			}

			@Override
			public Object getNewValue() {
				return offering.getStartStr();
			}

			@Override
			public Object getOldValue() {
				return null;
			}

			@Override
			public void apply(CourseModel model) {
				course.addOffering(offering);
			}
		};
		return change.setOrder(3);
	}

	public static Change delete(final OffRec offering) {
		final CourseRec course = offering.getCourse();
		Change change = new Change(course.getSource(), Change.DELETE) {
			@Override
			public String getDescription() {
				return "Offering";
			}

			@Override
			public Object getTarget() {
				return "[" + course.getStatus() + "]" + course.getName() + "("
						+ offering.getId() + ")";
			}

			@Override
			public Object getNewValue() {
				return null;
			}

			@Override
			public Object getOldValue() {
				return offering.getStartStr();
			}

			@Override
			public void apply(CourseModel model) {
				course.removeOffering(offering.getId());
			}
		};
		return change.setOrder(5);
	}

	private static abstract class OffFieldChange<T> extends FieldChange<T> {
		private OffRec offering;

		OffFieldChange(OffRec offering, String field, T newValue, T oldValue) {
			super(offering.getCourse().getSource(), field, newValue, oldValue);
			this.offering = offering;
		}

		@Override
		public Object getTarget() {
			CourseRec course = offering.getCourse();
			return "[" + course.getStatus() + "]" + course.getName() + "("
					+ offering.getId() + ")";
		}
	}

	public static Change setStart(final OffRec offering, final Date start) {
		Change change = new OffFieldChange<Date>(offering, "Start", start,
				offering.getStart()) {
			private String safeString(Object date) {
				if (date == null)
					return null;
				return OffRec.dformat.format(date);
			}

			@Override
			public Object getNewValue() {
				return safeString(super.getNewValue());
			}

			@Override
			public Object getOldValue() {
				return safeString(super.getOldValue());
			}

			@Override
			public void apply(CourseModel model) {
				offering.setStart(start);
				offering.setStartStr(null);
			}
		};
		return change.setOrder(4);
	}

	public static Change setStartStr(final OffRec offering,
			final String newValue) {
		Change change = new OffFieldChange<String>(offering, "Start string",
				newValue, offering.getStartStr()) {
			@Override
			public void apply(CourseModel model) {
				System.err
						.println("Setting start string, but not start date on "
								+ offering);
				offering.setStartStr(newValue);
			}
		};
		return change.setOrder(4);
	}

	public static Change setLink(final OffRec offering, final String newValue) {
		Change change = new OffFieldChange<String>(offering, "Link", newValue,
				offering.getLink()) {
			@Override
			public void apply(CourseModel model) {
				offering.setLink(newValue);
			}
		};
		return change.setOrder(4);
	}

	public static Change setDuration(final OffRec offering, final Long newValue) {
		Change change = new OffFieldChange<Long>(offering, "Duration",
				newValue, offering.getDuration()) {
			@Override
			public void apply(CourseModel model) {
				offering.setDuration(newValue);
			}
		};
		return change.setOrder(4);
	}

	public static Change setActive(final OffRec offering, final Boolean newValue) {
		Change change = new OffFieldChange<Boolean>(offering, "Active",
				newValue, offering.isActive()) {
			@Override
			public void apply(CourseModel model) {
				offering.setActive(newValue);
			}

			@Override
			public boolean isActivation() {
				return true;
			}
		};
		return change.setOrder(4);
	}
}
