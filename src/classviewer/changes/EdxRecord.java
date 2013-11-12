package classviewer.changes;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Result of parsing EdX list.
 * 
 * @author TK
 */
public class EdxRecord {
	public static SimpleDateFormat dformat1 = new SimpleDateFormat(
			"dd MMM yyyy");
	public static SimpleDateFormat dformat3 = new SimpleDateFormat(
			"MMMMM yyyy");

	private String courseId;
	private String name;
	private String descr;
	private String univer;
	private String home;
	private Date date;
	private int duration;
	private boolean isNew;

	public EdxRecord(String courseId, String name, String descr, String univer,
			Date date, int duration, String home, boolean isNew) {
		this.courseId = courseId;
		this.name = name;
		this.descr = descr;
		this.univer = univer;
		this.date = date;
		this.duration = duration;
		this.home = home;
		this.isNew = isNew;
	}

	public String toString() {
		return courseId + ", " + name + "\n\t" + descr + "\n\t" + univer + ", "
				+ (date==null?"No start":dformat1.format(date)) + ", " + duration + " weeks, " + home + ", " + isNew;

	}

	public String getUniversity() {
		return univer;
	}

	public String getCourseId() {
		return courseId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return descr;
	}

	public String getHome() {
		return home;
	}

	public Date getStart() {
		return date;
	}
	
	public static Date parseDate(String str) {
		if (str == null)
			return null;
		str = str.replace("<span class=\"date-display-single\">", "");
		str = str.replace(",", "");
		str = str.replace("Sept ", "Sep "); // More of these?
		
		try {
			return dformat1.parse(str);
		} catch (Exception e) {
		}
		try {
			return dformat3.parse(str);
		} catch (Exception e) {
			System.err.println("Cannot parse date " + str);
		}
		return null;
	}

	public int getDuration() {
		return duration;
	}

	public void setCourseId(String courseId) {
		this.courseId = courseId;
	}
}
