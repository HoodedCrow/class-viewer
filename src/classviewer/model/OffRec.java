package classviewer.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Offering
 * 
 * @author TK
 */
public class OffRec implements Linked {
	public static SimpleDateFormat dformat = new SimpleDateFormat("dd MMM yyyy");

	private int id, duration, spread;
	private Status status = Status.UNKNOWN;
	private Date start;
	private String home, startStr;
	private boolean active;
	private CourseRec course;

	public OffRec(int id, Date start, int duration, int spread, String home,
			boolean active, String startStr) {
		this.id = id;
		this.start = start;
		this.duration = duration;
		this.spread = spread;
		this.home = home == null ? "" : home;
		this.active = active;
		this.startStr = startStr == null ? "" : startStr;
	}

	public void updateId(int newId) {
		// TODO temporary disabling this check. assert (this.id == 0);
		this.id = newId;
	}

	public Status getStatus() {
		return status;
	}

	/**
	 * * An offering set to: S=yes, done, or registered - the course is set to
	 * S, unless higher, all other offerings that are Unknown or maybe are set
	 * to no
	 */
	public void setStatus(Status status) {
		if (this.status == status)
			return; // to avoid deep checks
		this.status = status;

		// Should we change course status?
		Status cs = course.getStatus().updateByOffering(status);
		if (cs != course.getStatus())
			course.setStatus(cs);
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

	public void setStatusDirect(Status stat) {
		this.status = stat;
	}

	public String getLongHtml() {
		String str = "Offering <b>" + id + "</b>, start ";
		if (startStr != null)
			str = str + "<b>" + startStr + "</b>";
		else if (start != null)
			str = str + "<b>" + dformat.format(start) + "</b>";
		else
			str = str + "tba";
		str = str + ", duration: <b>" + duration + "</b> weeks";
		if (active)
			str = str + ", active";
		if (home != null)
			str += "<br/><a href=\"" + home + "\">" + home + "</a>";
		return str;
	}
}
