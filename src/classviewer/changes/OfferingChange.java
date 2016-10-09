package classviewer.changes;

import java.util.Date;

import classviewer.model.CourseModel;
import classviewer.model.CourseRec;
import classviewer.model.OffRec;
import classviewer.model.Status;

public final class OfferingChange {
	private static void appendCourse(StringBuffer b, CourseRec record) {
		b.append(record.getName()).append("[").append(record.getStatus())
				.append("]<br/>");
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
		b.append("<div width=500px>").append(record.getDescription())
				.append("</div>");
	}
	
	public static Change add(final CourseRec course, final OffRec offering) {
		offering.setCourse(course);
		Change change = new Change(course.getSource(), Change.ADD, offering) {
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
				// Does class already have a MAYBE or REGISTERED offering?
				boolean alreadyHas = false;
				for (OffRec or : course.getOfferings()) {
					if (or.getStatus() == Status.MAYBE
							|| or.getStatus() == Status.REGISTERED) {
						alreadyHas = true;
						break;
					}
				}
				// Add first to avoid exception in setStatus.
				course.addOffering(offering);
				Status sc = course.getStatus();
				if (alreadyHas || sc == Status.NO || sc == Status.DONE
						|| sc == Status.AUDITED || sc == Status.CHAIN)
					offering.setStatus(Status.NO);
			}
		};
		StringBuffer b = new StringBuffer("<html><b>New offerring");
		if (offering.getStart() != null)
			b.append(" on ").append(OffRec.dformat.format(offering.getStart()));
		b.append("<br/>for ");
		appendCourse(b, course);
		return change.setOrder(3).setToolTop(b.toString());
	}

	public static Change delete(final OffRec offering) {
		final CourseRec course = offering.getCourse();
		Change change = new Change(course.getSource(), Change.DELETE, offering) {
			@Override
			public String getDescription() {
				return "Offering";
			}

			@Override
			public Object getTarget() {
				return "[" + course.getStatus() + offering.getStatus() + "]"
						+ course.getName() + "(" + offering.getId() + ")";
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
		StringBuffer b = new StringBuffer("<html><b>Delete offerring");
		if (offering.getStart() != null)
			b.append(" on ").append(OffRec.dformat.format(offering.getStart()));
		b.append("<br/>from ");
		appendCourse(b, course);
		return change.setOrder(5).setToolTop(b.toString());
	}

	private static abstract class OffFieldChange<T> extends FieldChange<T> {
		private OffRec offering;

		OffFieldChange(OffRec offering, String field, T newValue, T oldValue) {
			super(offering.getCourse().getSource(), field, newValue, oldValue,
					offering);
			this.offering = offering;
			StringBuffer b = new StringBuffer("<html><b>Change ");
			b.append(field).append("<br/>from ").append(oldValue)
					.append("<br/>to ").append(newValue).append("<br/>");
			if (offering.getStart() != null)
				b.append("for session starting on ")
						.append(OffRec.dformat.format(offering.getStart()))
						.append("<br/>");
			b.append("of ");
			appendCourse(b, offering.getCourse());
			setToolTop(b.toString());
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
		StringBuffer b = new StringBuffer("<html><b>Change Start<br/>from ");
		if (offering.getStart() != null)
			b.append(OffRec.dformat.format(offering.getStart()));
		b.append("<br/>to ");
		if (start != null)
			b.append(OffRec.dformat.format(start));
		b.append("<br/>for a session of ");
		appendCourse(b, offering.getCourse());
		return change.setOrder(4).setToolTop(b.toString());
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
