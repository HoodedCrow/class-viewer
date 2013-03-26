package classviewer.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Offering
 * 
 * @author TK
 */
public class OffRec implements Linked {
	private static SimpleDateFormat dformat = new SimpleDateFormat(
			"dd MMM yyyy");

	private int id, duration, spread;
	private Status status;
	private Date start;
	private String home, startStr, durStr;
	private boolean active;
	private CourseRec course;

	public OffRec(int id, Date start, int duration, int spread, 
			String home, boolean active, String startStr, String durStr) {
		this.id = id;
		this.start = start;
		this.duration = duration;
		this.spread = spread;
		this.home = home == null ? "" : home;
		this.active = active;
		this.startStr = startStr == null ? "" : startStr;
		this.durStr = durStr == null ? "" : durStr;
	}

	public Status getStatus() {
		return status;
	}

	/**
	 * * An offering set to: S=yes, done, or registered - the course is set to
	 * S, unless higher, all other offerings that are dunno or maybe are set to
	 * no
	 */
	public void setStatus(Status status) {
		this.status = status;
		/*
		// dunno, no, and maybe have nothing else to do
		if (status == StatusEnum.dunno.code() || status == StatusEnum.no.code()
				|| status == StatusEnum.maybe.code())
			return;
		if (status == StatusEnum.yes.code()
				&& course.getStatus() < StatusEnum.done.code())
			course.setStatus(status); // yes
		if (status == StatusEnum.registered.code()
				&& course.getStatus() != StatusEnum.done.code())
			course.setStatus(status); // registered
		if (status == StatusEnum.done.code())
			course.setStatus(status); // done

		// All dunno and maybe offerings are now NO
		for (OffRec o : course.getOfferings())
			if (o.getStatus() == StatusEnum.dunno.code()
					|| o.getStatus() == StatusEnum.maybe.code())
				o.setStatus(StatusEnum.no.code());
				*/
	}

	public int getId() {
		return id;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getSpread() {
		return spread;
	}

	public void setSpread(int spread) {
		this.spread = spread;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public String getLink() {
		return home;
	}

	public void setLink(String home) {
		this.home = home;
	}

	public String getStartStr() {
		return startStr;
	}

	public void setStartStr(String startStr) {
		this.startStr = startStr;
	}

	public String getDurStr() {
		return durStr;
	}

	public void setDurStr(String durStr) {
		this.durStr = durStr;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public CourseRec getCourse() {
		return course;
	}

	public void setCourse(CourseRec course) {
		this.course = course;
	}

	public String asHTML() {
		String ss = startStr;
		if ((ss == null || ss.isEmpty()) && start != null)
			ss = dformat.format(start);
		return "<html>" + (active ? "<b>" : "") + ss + (active ? "</b>" : "");
		// + ", " + durStr;
	}

	@Override
	public String toString() {
		return id + ":" + course.getId() + ":" + status;
	}

//	public void setStatusDirect(int stat) {
//		this.status = stat;
//	}
/*
	public void diff(OffRec other, ArrayList<Change> changes) {
		assert (this.id == other.id);
		if (this.spread != other.spread) {
			changes.add(Change.changed("Spread changed", Change.Type.SPREAD,
					this, other.spread));
		}
		if (this.active != other.active) {
			changes.add(Change.changed("Active changed", Change.Type.ACTIVE,
					this, other.active));
		}
		if (!this.home.equals(other.home))
			changes.add(Change.changed("Link changed", Change.Type.LINK, this,
					other.home));
		if (!this.startStr.equals(other.startStr))
			changes.add(Change.changed("Start string changed",
					Change.Type.START, this, other));
		if (!this.durStr.equals(other.durStr))
			changes.add(Change.changed("Duration string changed",
	}
*/
}
